package com.lifegrouptext.di

import android.content.Context
import com.lifegrouptext.data.ContactRepository
import com.lifegrouptext.data.DraftRepository
import com.lifegrouptext.data.GroupRepository
import com.lifegrouptext.data.SendLogRepository
import com.lifegrouptext.data.db.LifeGroupDatabase
import com.lifegrouptext.sms.BulkSender
import com.lifegrouptext.sms.SmsSender

/**
 * Manual dependency container. Kept intentionally small: the database, the
 * repositories and the SMS stack wire up through one place instead of a DI framework.
 */
class AppContainer(private val appContext: Context) {

    private val database: LifeGroupDatabase by lazy { LifeGroupDatabase.build(appContext) }

    val contactRepository: ContactRepository by lazy {
        ContactRepository(appContext, database.contactDao())
    }

    val groupRepository: GroupRepository by lazy {
        GroupRepository(database.groupDao())
    }

    val draftRepository: DraftRepository by lazy {
        DraftRepository(database.draftDao())
    }

    val sendLogRepository: SendLogRepository by lazy {
        SendLogRepository(database.sendLogDao())
    }

    val smsSender: SmsSender by lazy { SmsSender(appContext) }

    val bulkSender: BulkSender by lazy { BulkSender(smsSender, sendLogRepository) }
}
