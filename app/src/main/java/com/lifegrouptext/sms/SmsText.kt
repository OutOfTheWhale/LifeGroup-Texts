package com.lifegrouptext.sms

/** Which alphabet a message will be encoded in on the wire. */
enum class SmsEncoding { GSM_7BIT, UCS2 }

/**
 * What a message costs to send.
 *
 * [units] counts septets for [SmsEncoding.GSM_7BIT] and UTF-16 code units for
 * [SmsEncoding.UCS2] — the two are not comparable, which is exactly why a plain
 * "characters typed" counter misleads.
 */
data class SmsMetrics(
    val encoding: SmsEncoding,
    val units: Int,
    val segments: Int,
    val unitsPerSegment: Int,
    val remaining: Int,
)

/**
 * Encoding rules for GSM 03.38, the alphabet SMS uses.
 *
 * The short version, and the reason messages used to fail silently: a message made
 * only of characters in the GSM alphabet fits 160 characters in one segment. Introduce
 * a single character outside it — an emoji, a curly apostrophe pasted from a phone
 * keyboard — and the whole message switches to UCS-2, where a segment holds just 70.
 * Concatenated messages give up a few units per segment to the header that reassembles
 * them, which is why the multipart limits are 153 and 67 rather than 160 and 70.
 */
object SmsText {

    const val GSM_SINGLE = 160
    const val GSM_MULTIPART = 153
    const val UCS2_SINGLE = 70
    const val UCS2_MULTIPART = 67

    /** GSM 03.38 basic table, one septet each. ESC (0x1B) is omitted: it is a prefix, not a character. */
    private const val GSM_BASIC =
        "@£\$¥èéùìòÇ\nØø\rÅå" +
            "Δ_ΦΓΛΩΠΨΣΘΞÆæßÉ" +
            " !\"#¤%&'()*+,-./" +
            "0123456789:;<=>?" +
            "¡ABCDEFGHIJKLMNO" +
            "PQRSTUVWXYZÄÖÑÜ§" +
            "¿abcdefghijklmno" +
            "pqrstuvwxyzäöñüà"

    /** Reachable only via an escape prefix, so each of these costs two septets. */
    private const val GSM_EXTENDED = "^{}\\[~]|€"

    private val basic: Set<Char> = GSM_BASIC.toHashSet()
    private val extended: Set<Char> = GSM_EXTENDED.toHashSet()

    /** True when [ch] can be sent in the GSM alphabet at all. */
    fun isGsm(ch: Char): Boolean = ch in basic || ch in extended

    /** Septets [ch] occupies: 2 for the escaped characters, 1 for the rest. */
    private fun cost(ch: Char): Int = if (ch in extended) 2 else 1

    fun encodingOf(text: String): SmsEncoding =
        if (text.all(::isGsm)) SmsEncoding.GSM_7BIT else SmsEncoding.UCS2

    /**
     * The characters in [text] that force the whole message to UCS-2, de-duplicated
     * and in the order they first appear. Emoji arrive here as surrogate halves, so
     * they are reported as whole code points instead.
     */
    fun nonGsmCharacters(text: String): List<String> {
        val seen = LinkedHashSet<String>()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val width = Character.charCount(cp)
            if (width > 1 || !isGsm(text[i])) seen += text.substring(i, i + width)
            i += width
        }
        return seen.toList()
    }

    /**
     * Replace the punctuation that phone and desktop keyboards substitute automatically
     * — curly quotes, en/em dashes, ellipsis, non-breaking spaces — with the plain
     * equivalents the GSM alphabet actually has, and normalize line endings.
     *
     * Escapes rather than literals below, because most of these characters are either
     * invisible or indistinguishable from their plain counterpart in an editor.
     *
     * Emoji are deliberately left alone: they send fine as UCS-2, just at 70 characters
     * per segment. [stripNonGsm] is there for when the caller would rather drop them.
     */
    fun sanitize(text: String): String {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val out = StringBuilder(normalized.length)
        for (ch in normalized) {
            when (ch) {
                // Curly single quotes, low/high-reversed quotes, prime.
                '‘', '’', '‚', '‛', '′' -> out.append('\'')
                // Curly double quotes, low/high-reversed quotes, double prime.
                '“', '”', '„', '‟', '″' -> out.append('"')
                // Hyphen, non-breaking hyphen, figure/en/em/horizontal dash, minus sign.
                '‐', '‑', '‒', '–', '—', '―', '−' ->
                    out.append('-')
                '…' -> out.append("...")                          // ellipsis
                '•', '·', '●' -> out.append('-')        // bullets
                '⁄' -> out.append('/')                            // fraction slash
                // Non-breaking, figure, thin, hair, narrow no-break and ideographic spaces.
                ' ', ' ', ' ', ' ', ' ', '　' -> out.append(' ')
                // Zero-width space/non-joiner/joiner, BOM and variation selectors: these
                // ride along with emoji and pasted text, render as nothing, and still
                // force the whole message to UCS-2.
                '​', '‌', '‍', '﻿', '︎', '️' -> Unit
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    /**
     * [sanitize], then drop whatever still falls outside the GSM alphabet, so the
     * message is guaranteed to send at 160 characters per segment.
     */
    fun stripNonGsm(text: String): String {
        val cleaned = sanitize(text)
        val out = StringBuilder(cleaned.length)
        for (ch in cleaned) if (isGsm(ch)) out.append(ch)
        // Emoji usually sit between words; removing one leaves a double space behind.
        return out.toString().replace(Regex("[ \\t]{2,}"), " ").trim()
    }

    /** Total septets, counting the escaped characters twice. */
    fun septets(text: String): Int = text.sumOf { cost(it) }

    /** Measure [text] as it will actually be sent. */
    fun measure(text: String): SmsMetrics {
        if (text.isEmpty()) {
            return SmsMetrics(SmsEncoding.GSM_7BIT, 0, 0, GSM_SINGLE, GSM_SINGLE)
        }
        return when (encodingOf(text)) {
            SmsEncoding.GSM_7BIT -> {
                val units = septets(text)
                if (units <= GSM_SINGLE) {
                    SmsMetrics(SmsEncoding.GSM_7BIT, units, 1, GSM_SINGLE, GSM_SINGLE - units)
                } else {
                    val segments = packGsm(text)
                    SmsMetrics(
                        encoding = SmsEncoding.GSM_7BIT,
                        units = units,
                        segments = segments,
                        unitsPerSegment = GSM_MULTIPART,
                        remaining = segments * GSM_MULTIPART - units,
                    )
                }
            }

            SmsEncoding.UCS2 -> {
                val units = text.length
                if (units <= UCS2_SINGLE) {
                    SmsMetrics(SmsEncoding.UCS2, units, 1, UCS2_SINGLE, UCS2_SINGLE - units)
                } else {
                    val segments = packUcs2(text)
                    SmsMetrics(
                        encoding = SmsEncoding.UCS2,
                        units = units,
                        segments = segments,
                        unitsPerSegment = UCS2_MULTIPART,
                        remaining = segments * UCS2_MULTIPART - units,
                    )
                }
            }
        }
    }

    /** Convenience for callers that only want the segment count. */
    fun segments(text: String): Int = measure(text).segments

    /**
     * Fill 153-septet segments without letting an escaped character straddle a
     * boundary — its prefix and value have to land in the same segment.
     */
    private fun packGsm(text: String): Int {
        var segments = 1
        var used = 0
        for (ch in text) {
            val cost = cost(ch)
            if (used + cost > GSM_MULTIPART) {
                segments++
                used = 0
            }
            used += cost
        }
        return segments
    }

    /**
     * Fill 67-unit segments without splitting a surrogate pair, so an emoji is never
     * torn in half across two texts.
     */
    private fun packUcs2(text: String): Int {
        var segments = 1
        var used = 0
        var i = 0
        while (i < text.length) {
            val width = Character.charCount(text.codePointAt(i))
            if (used + width > UCS2_MULTIPART) {
                segments++
                used = 0
            }
            used += width
            i += width
        }
        return segments
    }
}
