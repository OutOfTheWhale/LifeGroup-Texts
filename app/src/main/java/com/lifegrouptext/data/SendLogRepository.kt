package com.lifegrouptext.data

import com.lifegrouptext.data.db.SendLogDao
import com.lifegrouptext.data.db.SendLogEntity
import com.lifegrouptext.domain.SendLogEntry
import com.lifegrouptext.domain.SendStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** History of what was actually sent, and what failed. */
class SendLogRepository(private val dao: SendLogDao) {

    fun observeAll(): Flow<List<SendLogEntry>> =
        dao.observeAll().map { list -> list.map { it.toEntry() } }

    suspend fun record(entries: List<SendLogEntry>) {
        if (entries.isEmpty()) return
        dao.insertAll(entries.map { it.toEntity() })
    }

    suspend fun clear() = dao.clear()

    private fun SendLogEntity.toEntry() = SendLogEntry(
        id = id,
        contactName = contactName,
        phone = phone,
        body = body,
        sentAt = sentAt,
        status = runCatching { SendStatus.valueOf(status) }.getOrDefault(SendStatus.FAILED),
        segments = segments,
        failureReason = failureReason,
    )

    private fun SendLogEntry.toEntity() = SendLogEntity(
        contactName = contactName,
        phone = phone,
        body = body,
        sentAt = sentAt,
        status = status.name,
        segments = segments,
        failureReason = failureReason,
    )
}
