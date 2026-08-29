package com.lifegrouptext.domain

/**
 * Phone numbers are stored as digits only so that the same person typed as
 * "(361) 555-0101", "361-555-0101" or "+1 361 555 0101" compares equal.
 */
object PhoneNumber {

    /** Strip everything but digits. A leading US country code is kept as typed. */
    fun normalize(raw: String): String = raw.filter { it.isDigit() }

    /**
     * Two numbers match if their last ten digits agree, so a contact saved with a
     * "+1" country code still de-duplicates against the same number saved without.
     */
    fun sameNumber(a: String, b: String): Boolean {
        val x = normalize(a).takeLast(10)
        val y = normalize(b).takeLast(10)
        return x.isNotEmpty() && x == y
    }

    /** True once there are enough digits to be a dialable number (area code included). */
    fun isValid(raw: String): Boolean = normalize(raw).length >= 10
}
