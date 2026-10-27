package io.github.rajumark.sextant.core

import com.android.apksig.ApkVerifier
import io.github.rajumark.sextant.core.model.Certificate
import io.github.rajumark.sextant.core.model.Signing
import java.nio.file.Path
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Reads APK signature schemes and signer certificates with Google's apksig. */
object SigningReader {
    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    fun read(
        path: Path,
        warnings: MutableList<String>,
    ): Signing {
        val result =
            try {
                ApkVerifier.Builder(path.toFile()).build().verify()
            } catch (e: Exception) {
                warnings += "signature could not be read (${e.javaClass.simpleName}: ${e.message})"
                return Signing(verified = false, schemes = emptyList(), certificates = emptyList())
            }
        val schemes =
            buildList {
                if (result.isVerifiedUsingV1Scheme) add("v1")
                if (result.isVerifiedUsingV2Scheme) add("v2")
                if (result.isVerifiedUsingV3Scheme) add("v3")
                if (result.isVerifiedUsingV31Scheme) add("v3.1")
            }
        if (!result.isVerified) {
            val first = result.allErrors.firstOrNull()?.toString()
            warnings += if (first == null) "APK is not signed" else "signature did not verify: $first"
        }
        val certificates = result.signerCertificates.map(::certificate).distinctBy { it.sha256 }
        return Signing(verified = result.isVerified, schemes = schemes, certificates = certificates)
    }

    private fun certificate(cert: X509Certificate) =
        Certificate(
            sha256 = fingerprint(cert, "SHA-256"),
            sha1 = fingerprint(cert, "SHA-1"),
            subject = cert.subjectX500Principal.name,
            issuer = cert.issuerX500Principal.name,
            notBefore = DATE.format(cert.notBefore.toInstant()),
            notAfter = DATE.format(cert.notAfter.toInstant()),
        )

    private fun fingerprint(
        cert: X509Certificate,
        algorithm: String,
    ) = MessageDigest.getInstance(algorithm).digest(cert.encoded).joinToString(":") { "%02X".format(it) }
}
