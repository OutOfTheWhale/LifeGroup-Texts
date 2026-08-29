package com.lifegrouptext.domain

/**
 * Does this contact match what the user typed?
 *
 * The digit clause is guarded deliberately. `PhoneNumber.normalize` strips a query like
 * "pop" down to "", and every string contains "" — so an unguarded `phone.contains(...)`
 * matches every contact and the search silently returns the whole address book.
 */
fun contactMatches(name: String, phone: String, query: String): Boolean {
    if (query.isBlank()) return true
    if (name.contains(query, ignoreCase = true)) return true
    val digits = PhoneNumber.normalize(query)
    return digits.isNotEmpty() && PhoneNumber.normalize(phone).contains(digits)
}
