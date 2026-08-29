package com.lifegrouptext.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["phone"], unique = true)],
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Digits only — see `PhoneNumber.normalize`. */
    val phone: String,
    val addedAt: Long,
)

/** Named `contact_groups`: `GROUPS` is a keyword in SQLite's window-function syntax. */
@Entity(tableName = "contact_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/**
 * Junction table for group membership. Both sides cascade, so deleting a contact or a
 * group cleans up its memberships instead of leaving orphan rows behind — the bug the
 * old app worked around by hand-filtering `contactIds` after every delete.
 */
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "contactId"],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contactId")],
)
data class GroupMemberEntity(
    val groupId: Long,
    val contactId: Long,
)

/**
 * The message being composed. A single row (id 0) rather than a list — the old app's
 * many-boxes model existed only to work around single-segment sending.
 */
@Entity(tableName = "draft")
data class DraftEntity(
    @PrimaryKey val id: Int = 0,
    val body: String,
    val updatedAt: Long,
)

@Entity(tableName = "send_log")
data class SendLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phone: String,
    val body: String,
    val sentAt: Long,
    /** Stored as the name of a `SendStatus`. */
    val status: String,
    val segments: Int,
    val failureReason: String?,
)
