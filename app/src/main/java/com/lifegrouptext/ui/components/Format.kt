package com.lifegrouptext.ui.components

import com.lifegrouptext.domain.PhoneNumber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Display form for a stored (digits-only) number. Falls back to the raw digits. */
fun formatPhone(stored: String): String {
    val digits = PhoneNumber.normalize(stored)
    return when {
        digits.length == 10 ->
            "(${digits.take(3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"

        digits.length == 11 && digits.startsWith("1") ->
            "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"

        else -> stored
    }
}

private val timestampFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

fun formatTimestamp(millis: Long): String = timestampFormat.format(Date(millis))

/** "3 people" / "1 person" — used all over the recipient UI. */
fun countOf(n: Int, singular: String, plural: String = singular + "s"): String =
    "$n ${if (n == 1) singular else plural}"
