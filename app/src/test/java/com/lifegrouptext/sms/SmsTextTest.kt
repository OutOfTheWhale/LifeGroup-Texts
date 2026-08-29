package com.lifegrouptext.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These cover the two bugs that made the old app's messages fail: single-segment
 * sending, and unnoticed UCS-2 promotion by a stray emoji or curly apostrophe.
 */
class SmsTextTest {

    private fun ascii(n: Int) = "a".repeat(n)

    @Test
    fun `plain text stays in the GSM alphabet`() {
        assertEquals(SmsEncoding.GSM_7BIT, SmsText.encodingOf("Hi Mary, service is at 10am."))
    }

    @Test
    fun `160 GSM characters is one segment`() {
        val m = SmsText.measure(ascii(160))
        assertEquals(1, m.segments)
        assertEquals(160, m.units)
        assertEquals(0, m.remaining)
    }

    @Test
    fun `161 GSM characters spills into two segments of 153`() {
        val m = SmsText.measure(ascii(161))
        assertEquals(2, m.segments)
        assertEquals(153, m.unitsPerSegment)
        assertEquals(306 - 161, m.remaining)
    }

    @Test
    fun `306 GSM characters is exactly two segments`() {
        assertEquals(2, SmsText.segments(ascii(306)))
        assertEquals(3, SmsText.segments(ascii(307)))
    }

    @Test
    fun `a long message sends as several segments rather than failing`() {
        // The case the old app could not handle at all.
        val m = SmsText.measure(ascii(500))
        assertEquals(SmsEncoding.GSM_7BIT, m.encoding)
        assertEquals(4, m.segments)
    }

    @Test
    fun `one emoji drops the whole message to UCS-2 at 70 per segment`() {
        val m = SmsText.measure(ascii(100) + "🙏") // 🙏
        assertEquals(SmsEncoding.UCS2, m.encoding)
        assertEquals(102, m.units) // the emoji is a surrogate pair
        assertEquals(67, m.unitsPerSegment)
        assertEquals(2, m.segments)
    }

    @Test
    fun `the old default message is UCS-2 because of its emoji`() {
        val old = "Hi {name}, just a reminder about Sunday service at 10am. God bless! 🙏"
        assertEquals(SmsEncoding.UCS2, SmsText.encodingOf(old))
        assertEquals(SmsEncoding.GSM_7BIT, SmsText.encodingOf(SmsText.stripNonGsm(old)))
    }

    @Test
    fun `70 UCS-2 units is one segment and 71 is two`() {
        val accented = "é".repeat(70) // é is GSM, so use a character that is not
        assertEquals(SmsEncoding.GSM_7BIT, SmsText.encodingOf(accented))

        val cyrillic = "д".repeat(70)
        assertEquals(1, SmsText.segments(cyrillic))
        assertEquals(2, SmsText.segments("д".repeat(71)))
    }

    @Test
    fun `curly punctuation is rewritten so the message stays GSM`() {
        val typed = "Don’t forget — it’s at 10…"
        assertEquals(SmsEncoding.UCS2, SmsText.encodingOf(typed))

        val cleaned = SmsText.sanitize(typed)
        assertEquals("Don't forget - it's at 10...", cleaned)
        assertEquals(SmsEncoding.GSM_7BIT, SmsText.encodingOf(cleaned))
    }

    @Test
    fun `non-breaking and zero-width characters are normalized away`() {
        val messy = "a b​c﻿"
        assertEquals("a bc", SmsText.sanitize(messy))
        assertEquals(SmsEncoding.GSM_7BIT, SmsText.encodingOf(SmsText.sanitize(messy)))
    }

    @Test
    fun `line endings are normalized`() {
        assertEquals("a\nb\nc", SmsText.sanitize("a\r\nb\rc"))
    }

    @Test
    fun `sanitize leaves emoji alone`() {
        val withEmoji = "Bless you 🙏"
        assertEquals(withEmoji, SmsText.sanitize(withEmoji))
    }

    @Test
    fun `stripNonGsm removes emoji and tidies the gap it leaves`() {
        assertEquals("See you Sunday!", SmsText.stripNonGsm("See you 🙏 Sunday!"))
    }

    @Test
    fun `escaped characters cost two septets`() {
        assertEquals(2, SmsText.septets("{"))
        assertEquals(1, SmsText.septets("a"))
        // 80 braces fill a 160-septet segment exactly.
        assertEquals(1, SmsText.segments("{".repeat(80)))
        assertEquals(2, SmsText.segments("{".repeat(81)))
    }

    @Test
    fun `an escaped character is not split across a segment boundary`() {
        // 152 plain septets leaves one free, but a brace needs two, so it moves along.
        val m = SmsText.measure(ascii(152) + "{" + ascii(200))
        assertTrue(m.segments >= 3)
        assertEquals(SmsEncoding.GSM_7BIT, m.encoding)
    }

    @Test
    fun `nonGsmCharacters reports whole emoji and de-duplicates`() {
        val found = SmsText.nonGsmCharacters("a🙏b🙏’")
        assertEquals(listOf("🙏", "’"), found)
    }

    @Test
    fun `plain text reports no offending characters`() {
        assertTrue(SmsText.nonGsmCharacters("Sunday at 10am!").isEmpty())
    }

    @Test
    fun `empty message costs nothing`() {
        val m = SmsText.measure("")
        assertEquals(0, m.units)
        assertEquals(0, m.segments)
    }

    @Test
    fun `the euro sign is GSM but the degree sign is not`() {
        assertTrue(SmsText.isGsm('€'))
        assertFalse(SmsText.isGsm('°'))
    }
}
