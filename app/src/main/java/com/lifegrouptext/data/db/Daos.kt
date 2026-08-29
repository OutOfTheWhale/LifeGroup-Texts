package com.lifegrouptext.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface GroupDao {

    @Query("SELECT * FROM contact_groups ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM group_members")
    fun observeAllMembers(): Flow<List<GroupMemberEntity>>

    @Query("SELECT contactId FROM group_members WHERE groupId IN (:groupIds)")
    suspend fun memberIdsOf(groupIds: List<Long>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity): Long

    @Update
    suspend fun update(group: GroupEntity)

    @Query("DELETE FROM contact_groups WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMember(member: GroupMemberEntity)

    @Delete
    suspend fun removeMember(member: GroupMemberEntity)
}

@Dao
interface DraftDao {

    @Query("SELECT * FROM draft WHERE id = 0")
    fun observe(): Flow<DraftEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(draft: DraftEntity)
}

@Dao
interface SendLogDao {

    @Query("SELECT * FROM send_log ORDER BY sentAt DESC, id DESC")
    fun observeAll(): Flow<List<SendLogEntity>>

    @Insert
    suspend fun insertAll(entries: List<SendLogEntity>)

    @Query("DELETE FROM send_log")
    suspend fun clear()
}
