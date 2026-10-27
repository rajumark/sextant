package io.github.rajumark.sextant.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.model.Certificate
import io.github.rajumark.sextant.desktop.theme.MonoFamily
import io.github.rajumark.sextant.desktop.ui.common.CopyButton
import io.github.rajumark.sextant.desktop.ui.common.KeyValueRow
import io.github.rajumark.sextant.desktop.ui.common.SectionCard
import io.github.rajumark.sextant.desktop.ui.common.Tag
import io.github.rajumark.sextant.desktop.ui.common.Tone
import io.github.rajumark.sextant.desktop.ui.workspace.WorkspaceState

private val SchemeInfo =
    listOf(
        "v1" to "JAR signing",
        "v2" to "APK Signature Scheme v2 · Android 7.0+",
        "v3" to "Key rotation support · Android 9+",
        "v4" to "Incremental install · Android 11+",
    )

@Composable
fun SigningScreen(ws: WorkspaceState) {
    val s = ws.analysis.meta.signing
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 1100.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ListHeader("Signing", "Read with apksig, Google's official verifier for schemes v1–v4") {
                when {
                    s.schemes.isEmpty() -> Tag("unsigned", Tone.Error)
                    s.verified -> Tag("signature verified", Tone.Primary)
                    else -> Tag("verification failed", Tone.Error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SchemeInfo.forEach { (id, desc) -> SchemeTile(id, desc, id in s.schemes, Modifier.weight(1f)) }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                s.certificates.forEachIndexed { i, cert -> CertificateCard(i, cert) }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp).fillMaxWidth()) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Two APKs signed with the same certificate fingerprint come from the same signer. " +
                            "Compare the SHA-256 value with the one published by the developer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SchemeTile(
    id: String,
    desc: String,
    present: Boolean,
    modifier: Modifier,
) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = if (present) c.primaryContainer else c.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    id,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (present) c.onPrimaryContainer else c.outline,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (present) Icons.Outlined.CheckCircle else Icons.Outlined.RemoveCircleOutline,
                    if (present) "Present" else "Not present",
                    tint = if (present) c.primary else c.outline,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (present) "Present" else "Not present",
                style = MaterialTheme.typography.labelLarge,
                color = if (present) c.onPrimaryContainer else c.onSurfaceVariant,
            )
            Text(desc, style = MaterialTheme.typography.bodySmall, color = if (present) c.onPrimaryContainer else c.onSurfaceVariant)
        }
    }
}

@Composable
private fun CertificateCard(
    index: Int,
    cert: Certificate,
) {
    SectionCard("Certificate ${index + 1}", icon = Icons.Outlined.Badge, action = {
        if (cert.subject == cert.issuer) Tag("self-signed", Tone.Secondary)
    }) {
        KeyValueRow("Subject", cert.subject, mono = true, copyable = true)
        KeyValueRow("Issuer", cert.issuer, mono = true)
        Spacer(Modifier.height(12.dp))
        ValidityBar(cert)
        Spacer(Modifier.height(16.dp))
        Fingerprint("SHA-256", cert.sha256)
        Spacer(Modifier.height(8.dp))
        Fingerprint("SHA-1", cert.sha1)
    }
}

@Composable
private fun Fingerprint(
    label: String,
    value: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily))
            }
            CopyButton(value, "$label fingerprint")
        }
    }
}

private fun year(iso: String): Int = iso.take(4).toIntOrNull() ?: 0

/** Validity window as a bar from not-before to not-after with a "now" marker. */
@Composable
private fun ValidityBar(cert: Certificate) {
    val c = MaterialTheme.colorScheme
    val start = year(cert.notBefore)
    val end = year(cert.notAfter)
    val now = 2026
    val fraction = ((now - start).toFloat() / (end - start).coerceAtLeast(1)).coerceIn(0f, 1f)
    Column {
        Row {
            Text("Valid from", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("until", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
        }
        Row {
            Text(
                cert.notBefore.take(10),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily),
                modifier = Modifier.weight(1f),
            )
            Text(cert.notAfter.take(10), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily))
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(c.surfaceContainerHighest),
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(c.primary),
            )
            Box(
                Modifier
                    .offset(x = (maxWidth - 16.dp) * fraction)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(c.primary)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(c.onPrimary),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Today is ${now - start} years into a ${end - start}-year validity window.",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
        )
    }
}
