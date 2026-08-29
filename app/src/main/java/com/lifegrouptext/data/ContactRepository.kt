package com.lifegrouptext.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.lifegrouptext.data.db.ContactDao
import com.lifegrouptext.data.db.ContactEntity
import com.lifegrouptext.domain.Contact
import com.lifegrouptext.domain.PhoneContact
import com.lifegrouptext.domain.PhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The app's own contact list, plus read-only access to the phone's. */
class ContactRepository(
    private val appContext: Context,
    private val dao: ContactDao,
) {

    fun observeAll(): Flow<List<Contact>> =
        dao.observeAll().map { list -> list.map { it.toContact() } }

    suspend fun byIds(ids: List<Long>): List<Contact> =
        if (ids.isEmpty()) emptyList() else dao.getByIds(ids).map { it.toContact() }

    /**
     * Add a contact, or return null when the number is already saved. Numbers are
     * normalized first so the same person entered two different ways collapses to one.
     */
    suspend fun add(name: String, rawPhone: String): Long? {
        val phone = PhoneNumber.normalize(rawPhone)
        if (name.isBlank() || phone.isEmpty()) return null
        if (dao.getAll().any { PhoneNumber.sameNumber(it.phone, phone) }) return null
        val id = dao.insert(
            ContactEntity(name = name.trim(), phone = phone, addedAt = System.currentTimeMillis()),
        )
        return id.takeIf { it != -1L }
    }

    suspend fun update(contact: Contact) {
        val existing = dao.getAll().firstOrNull { it.id == contact.id } ?: return
        dao.update(
            existing.copy(
                name = contact.name.trim(),
                phone = PhoneNumber.normalize(contact.phone),
            ),
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    /** True when [rawPhone] is already in the app's list. */
    suspend fun isSaved(rawPhone: String): Boolean =
        dao.getAll().any { PhoneNumber.sameNumber(it.phone, rawPhone) }

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Read the device's contact list. One person can have several numbers, so entries
     * are de-duplicated by number and the name is kept with each.
     */
    suspend fun readPhoneContacts(): List<PhoneContact> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()

        val found = LinkedHashMap<String, PhoneContact>()
        appContext.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)?.trim().orEmpty()
                val phone = PhoneNumber.normalize(cursor.getString(1).orEmpty())
                if (name.isEmpty() || phone.isEmpty()) continue
                val key = phone.takeLast(10)
                found.getOrPut(key) { PhoneContact(name = name, phone = phone) }
            }
        }
        found.values.toList()
    }

    private fun ContactEntity.toContact() = Contact(id = id, name = name, phone = phone)
}
