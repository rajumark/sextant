package io.github.rajumark.sextant.core

import com.android.apksig.ApkVerifier
import com.android.apksig.internal.apk.ApkSigningBlockUtils
import com.android.apksig.internal.apk.SignatureAlgorithm
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.DSAPublicKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** One signer of one scheme: v1 (a JAR signature file), v2, v3, v3.1 or v4, with its certificate and content digests. */
class SignerRow(
    val scheme: String,
    val index: Int,
    /** v1: the signature file (META-INF/CERT.SF); otherwise null. */
    val name: String?,
    /** v3 and v3.1: the SDK range this signer covers. */
    val minSdk: Int?,
    val maxSdk: Int?,
    val certSha256: String?,
    /** (signature algorithm, digest hex) of the signed content, v2/v3. */
    val digests: List<Pair<String, String>>,
)

/** One certificate, every field people check, plus its DER bytes. */
class CertRow(
    val sha256: String,
    val sha1: String,
    val md5: String,
    val subject: String,
    val issuer: String,
    val serial: String,
    val notBefore: String,
    val notAfter: String,
    val signatureAlgorithm: String,
    val keyAlgorithm: String,
    val keySize: Int?,
    val publicKeySha256: String,
    val der: ByteArray,
)

/** A verifier error, warning or info message, and where it applies. */
class IssueRow(
    val level: String,
    /** apk, v1:<name>, v2:<index>, v3:<index>, v3.1:<index>, source-stamp */
    val scope: String,
    val issue: String,
    val message: String,
)

/** Everything apksig reports, as rows: signers per scheme, certificates, rotation lineage, source stamp and issues. */
class SigningDetail(
    val signers: List<SignerRow> = emptyList(),
    val certificates: List<CertRow> = emptyList(),
    /** SHA-256 of each certificate in the key rotation lineage, oldest first. */
    val lineage: List<String> = emptyList(),
    val sourceStamp: String? = null,
    val issues: List<IssueRow> = emptyList(),
    /** (entry path, digest algorithm, digest) from META-INF/MANIFEST.MF. */
    val manifestDigests: List<Triple<String, String, String>> = emptyList(),
)

object SigningDetails {
    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    fun of(
        result: ApkVerifier.Result,
        manifestMf: ByteArray?,
    ): SigningDetail {
        val certs = LinkedHashMap<String, CertRow>()

        fun cert(c: X509Certificate?): String? = c?.let { x -> certificate(x).also { certs.putIfAbsent(it.sha256, it) }.sha256 }

        fun digests(list: List<ApkSigningBlockUtils.Result.SignerInfo.ContentDigest>): List<Pair<String, String>> =
            list.map { d ->
                (SignatureAlgorithm.findById(d.signatureAlgorithmId)?.name ?: "0x%04x".format(d.signatureAlgorithmId)) to
                    hex(d.value)
            }
        val signers = mutableListOf<SignerRow>()
        val issues = mutableListOf<IssueRow>()

        fun issues(
            scope: String,
            errors: List<ApkVerifier.IssueWithParams>,
            warnings: List<ApkVerifier.IssueWithParams>,
        ) {
            errors.forEach { issues += IssueRow("error", scope, it.issue.name, it.toString()) }
            warnings.forEach { issues += IssueRow("warning", scope, it.issue.name, it.toString()) }
        }
        result.v1SchemeSigners.forEachIndexed { i, s ->
            signers += SignerRow("v1", i, s.signatureFileName, null, null, cert(s.certificate), emptyList())
            issues("v1:${s.name}", s.errors, s.warnings)
        }
        result.v2SchemeSigners.forEach { s ->
            signers += SignerRow("v2", s.index, null, null, null, cert(s.certificate), digests(s.contentDigests))
            issues("v2:${s.index}", s.errors, s.warnings)
        }
        result.v3SchemeSigners.forEach { s ->
            signers += SignerRow("v3", s.index, null, s.minSdkVersion, s.maxSdkVersion, cert(s.certificate), digests(s.contentDigests))
            issues("v3:${s.index}", s.errors, s.warnings)
        }
        result.v31SchemeSigners.forEach { s ->
            signers += SignerRow("v3.1", s.index, null, s.minSdkVersion, s.maxSdkVersion, cert(s.certificate), digests(s.contentDigests))
            issues("v3.1:${s.index}", s.errors, s.warnings)
        }
        result.v4SchemeSigners.forEachIndexed { i, s ->
            signers += SignerRow("v4", i, null, null, null, cert(s.certificate), emptyList())
        }
        issues("apk", result.errors, result.warnings)
        val lineage =
            result.signingCertificateLineage
                ?.certificatesInLineage
                .orEmpty()
                .mapNotNull(::cert)
        val stamp = result.sourceStampInfo?.let { s -> cert(s.certificate).also { issues("source-stamp", s.errors, s.warnings) } }
        return SigningDetail(signers, certs.values.toList(), lineage, stamp, issues, manifestMf?.let(::manifestDigests).orEmpty())
    }

    /** `Name:` blocks of a JAR manifest with their `*-Digest` lines; continuation lines start with one space. */
    internal fun manifestDigests(bytes: ByteArray): List<Triple<String, String, String>> {
        val lines = mutableListOf<String>()
        for (line in bytes.toString(Charsets.UTF_8).split("\r\n", "\n", "\r")) {
            if (line.startsWith(" ") && lines.isNotEmpty()) lines[lines.lastIndex] += line.substring(1) else lines += line
        }
        val out = mutableListOf<Triple<String, String, String>>()
        var name: String? = null
        for (line in lines) {
            when {
                line.isEmpty() -> name = null
                line.startsWith("Name: ") -> name = line.removePrefix("Name: ")
                name != null && line.contains("-Digest: ") ->
                    out += Triple(name, line.substringBefore("-Digest: "), line.substringAfter("-Digest: "))
            }
        }
        return out
    }

    private fun certificate(c: X509Certificate): CertRow {
        val key = c.publicKey
        val size =
            when (key) {
                is RSAPublicKey -> key.modulus.bitLength()
                is ECPublicKey -> key.params.curve.field.fieldSize
                is DSAPublicKey -> key.params.p.bitLength()
                else -> null
            }
        return CertRow(
            sha256 = fingerprint(c.encoded, "SHA-256"),
            sha1 = fingerprint(c.encoded, "SHA-1"),
            md5 = fingerprint(c.encoded, "MD5"),
            subject = c.subjectX500Principal.name,
            issuer = c.issuerX500Principal.name,
            serial = c.serialNumber.toString(16),
            notBefore = DATE.format(c.notBefore.toInstant()),
            notAfter = DATE.format(c.notAfter.toInstant()),
            signatureAlgorithm = c.sigAlgName,
            keyAlgorithm = key.algorithm,
            keySize = size,
            publicKeySha256 = fingerprint(key.encoded, "SHA-256"),
            der = c.encoded,
        )
    }

    /** Same format as [io.github.rajumark.sextant.core.model.Certificate.sha256]: colon-separated upper-case hex. */
    private fun fingerprint(
        bytes: ByteArray,
        algorithm: String,
    ) = MessageDigest.getInstance(algorithm).digest(bytes).joinToString(":") { "%02X".format(it) }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}
