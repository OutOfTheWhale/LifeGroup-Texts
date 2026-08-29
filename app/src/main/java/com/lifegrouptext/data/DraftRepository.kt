package com.lifegrouptext.data

import com.lifegrouptext.data.db.DraftDao
import com.lifegrouptext.data.db.DraftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The one message being composed, persisted so it survives leaving the app. */
class DraftRepository(private val dao: DraftDao) {

    fun observe(): Flow<String> = dao.observe().map { it?.body.orEmpty() }

    suspend fun save(body: String) =
        dao.put(DraftEntity(body = body, updatedAt = System.currentTimeMillis()))
}
