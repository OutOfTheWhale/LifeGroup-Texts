package com.lifegrouptext.domain

/** A person we can text. [phone] is always stored normalized (digits only). */
data class Contact(
    val id: Long,
    val name: String,
    val phone: String,
) {
    /** First word of the name, used to fill `{name}` in a message. */
    val firstName: String get() = name.trim().substringBefore(' ').ifBlank { name.trim() }
}

/** A named set of contacts, e.g. "Sunday Life Group". */
data class Group(
    val id: Long,
    val name: String,
    val memberIds: List<Long>,
)

/** How a single send turned out. */
enum class SendStatus { SENT, FAILED }

/** One row of the send history — one entry per recipient per send. */
data class SendLogEntry(
    val id: Long,
    val contactName: String,
    val phone: String,
    val body: String,
    val sentAt: Long,
    val status: SendStatus,
    val segments: Int,
    val failureReason: String?,
)

/** A contact read off the device's own contact list, before it is imported. */
data class PhoneContact(
    val name: String,
    val phone: String,
)
