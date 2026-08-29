package com.lifegrouptext.domain

/** The token users type in a message to stand in for the recipient's first name. */
const val NAME_TOKEN = "{name}"

/**
 * Replace every `{name}` in [body] with [firstName]. Kotlin's [String.replace] swaps
 * all occurrences, so a message may use the token more than once.
 */
fun personalize(body: String, firstName: String): String =
    body.replace(NAME_TOKEN, firstName)

/** Fill the token with a stand-in so the composer can show a realistic preview. */
fun previewOf(body: String, sample: String?): String =
    personalize(body, sample?.takeIf { it.isNotBlank() } ?: "Friend")
