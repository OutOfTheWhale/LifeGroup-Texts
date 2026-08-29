package com.lifegrouptext.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContactEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        DraftEntity::class,
        SendLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LifeGroupDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun groupDao(): GroupDao
    abstract fun draftDao(): DraftDao
    abstract fun sendLogDao(): SendLogDao

    companion object {
        fun build(context: Context): LifeGroupDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LifeGroupDatabase::class.java,
                "lifegroup.db",
            )
                // No migrations yet — version 1. Room turns SQLite's foreign-key
                // enforcement on for us, so the cascades in GroupMemberEntity apply.
                .build()
    }
}
