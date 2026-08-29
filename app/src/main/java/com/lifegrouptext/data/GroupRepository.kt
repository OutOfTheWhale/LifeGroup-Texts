package com.lifegrouptext.data

import com.lifegrouptext.data.db.GroupDao
import com.lifegrouptext.data.db.GroupEntity
import com.lifegrouptext.data.db.GroupMemberEntity
import com.lifegrouptext.domain.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Named sets of contacts. Membership lives in its own table and cascades on delete. */
class GroupRepository(private val dao: GroupDao) {

    fun observeAll(): Flow<List<Group>> =
        combine(dao.observeAll(), dao.observeAllMembers()) { groups, members ->
            val byGroup = members.groupBy { it.groupId }
            groups.map { group ->
                Group(
                    id = group.id,
                    name = group.name,
                    memberIds = byGroup[group.id]?.map { it.contactId }.orEmpty(),
                )
            }
        }

    suspend fun add(name: String): Long? {
        if (name.isBlank()) return null
        return dao.insert(GroupEntity(name = name.trim(), createdAt = System.currentTimeMillis()))
    }

    suspend fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        dao.update(GroupEntity(id = id, name = name.trim(), createdAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun setMembership(groupId: Long, contactId: Long, member: Boolean) {
        val row = GroupMemberEntity(groupId = groupId, contactId = contactId)
        if (member) dao.addMember(row) else dao.removeMember(row)
    }

    /** Every contact id belonging to any of [groupIds], de-duplicated. */
    suspend fun membersOf(groupIds: List<Long>): Set<Long> =
        if (groupIds.isEmpty()) emptySet() else dao.memberIdsOf(groupIds).toSet()
}
