package io.github.rajumark.sextant.desktop.ui.common

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import io.github.rajumark.sextant.desktop.model.OutputFormat

enum class Syntax { Json, Xml, Markdown, Plain }

fun OutputFormat.syntax(): Syntax =
    when (this) {
        OutputFormat.Markdown -> Syntax.Markdown
        OutputFormat.Json, OutputFormat.JsonLines -> Syntax.Json
        OutputFormat.Binary -> Syntax.Plain
    }

/** Splits [text] into highlighted lines. Colors come from the scheme's accent roles. */
@Composable
fun rememberHighlighted(
    text: String,
    syntax: Syntax,
): List<AnnotatedString> {
    val scheme = MaterialTheme.colorScheme
    return remember(text, syntax, scheme) {
        text.trimEnd('\n').split('\n').map { highlightLine(it, syntax, scheme) }
    }
}

@Composable
fun highlighted(
    text: String,
    syntax: Syntax,
): AnnotatedString {
    val scheme = MaterialTheme.colorScheme
    return remember(text, syntax, scheme) {
        buildAnnotatedString {
            text.split('\n').forEachIndexed { i, line ->
                if (i > 0) append('\n')
                append(highlightLine(line, syntax, scheme))
            }
        }
    }
}

private val jsonToken = Regex("\"(?:\\\\.|[^\"\\\\])*\"(\\s*:)?|-?\\d+(?:\\.\\d+)?|true|false|null")
private val xmlToken = Regex("</?[\\w:.-]+|/?>|[\\w:.-]+(?==)|\"[^\"]*\"")

internal fun highlightLine(
    line: String,
    syntax: Syntax,
    c: ColorScheme,
): AnnotatedString =
    buildAnnotatedString {
        when (syntax) {
            Syntax.Plain -> append(line)
            Syntax.Markdown ->
                when {
                    line.startsWith("#") -> withStyle(SpanStyle(color = c.primary, fontWeight = FontWeight.Bold)) { append(line) }
                    line.startsWith("- ") -> {
                        withStyle(SpanStyle(color = c.tertiary)) { append("- ") }
                        append(line.substring(2))
                    }
                    else -> withStyle(SpanStyle(color = c.onSurfaceVariant)) { append(line) }
                }
            Syntax.Json ->
                tokenize(line, jsonToken) { token, match ->
                    when {
                        match.groupValues[1].isNotEmpty() -> {
                            withStyle(SpanStyle(color = c.primary)) { append(token.substringBeforeLast(':')) }
                            append(':')
                        }
                        token.startsWith('"') -> withStyle(SpanStyle(color = c.tertiary)) { append(token) }
                        else -> withStyle(SpanStyle(color = c.secondary, fontWeight = FontWeight.Medium)) { append(token) }
                    }
                }
            Syntax.Xml ->
                tokenize(line, xmlToken) { token, _ ->
                    when {
                        token.startsWith(
                            "<",
                        ) ||
                            token.endsWith(">") -> withStyle(SpanStyle(color = c.primary, fontWeight = FontWeight.Medium)) { append(token) }
                        token.startsWith('"') -> withStyle(SpanStyle(color = c.tertiary)) { append(token) }
                        else -> withStyle(SpanStyle(color = c.secondary)) { append(token) }
                    }
                }
        }
    }

private inline fun AnnotatedString.Builder.tokenize(
    line: String,
    regex: Regex,
    styled: AnnotatedString.Builder.(String, MatchResult) -> Unit,
) {
    var last = 0
    for (m in regex.findAll(line)) {
        if (m.range.first > last) append(line.substring(last, m.range.first))
        styled(m.value, m)
        last = m.range.last + 1
    }
    if (last < line.length) append(line.substring(last))
}
