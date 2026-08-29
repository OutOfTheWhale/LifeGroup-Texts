package com.lifegrouptext.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchTest {

    private fun match(query: String) = contactMatches("Aaron Cristan NLC", "3615550101", query)

    @Test
    fun `a word that is not in the name does not match`() {
        // The regression: normalize("pop") is "", and every string contains "", so the
        // phone clause used to match every contact in the address book.
        assertFalse(match("pop"))
        assertFalse(match("zzz"))
    }

    @Test
    fun `part of the name matches, ignoring case`() {
        assertTrue(match("aaron"))
        assertTrue(match("CRISTAN"))
        assertTrue(match("nlc"))
    }

    @Test
    fun `digits match against the number`() {
        assertTrue(match("361"))
        assertTrue(match("5550101"))
        assertFalse(match("999"))
    }

    @Test
    fun `a typed number matches however it is punctuated`() {
        assertTrue(match("(361) 555"))
        assertTrue(match("361-555-0101"))
    }

    @Test
    fun `a stored number matches however it is punctuated`() {
        assertTrue(contactMatches("Pop", "+1 (830) 483-1414", "8304831414"))
        assertTrue(contactMatches("Pop", "+1 (830) 483-1414", "483"))
    }

    @Test
    fun `a blank query matches everything`() {
        assertTrue(match(""))
        assertTrue(match("   "))
    }
}
