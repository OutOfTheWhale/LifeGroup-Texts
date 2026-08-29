package com.lifegrouptext.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifegrouptext.data.ContactRepository
import com.lifegrouptext.data.GroupRepository
import com.lifegrouptext.di.AppContainer
import com.lifegrouptext.domain.Contact
import com.lifegrouptext.domain.Group
import com.lifegrouptext.domain.PhoneNumber
import com.lifegrouptext.ui.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PeopleUiState(
    val contacts: List<Contact> = emptyList(),
    val groups: List<Group> = emptyList(),
)

class PeopleViewModel(
    private val contactRepository: ContactRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    val state: StateFlow<PeopleUiState> =
        combine(contactRepository.observeAll(), groupRepository.observeAll()) { contacts, groups ->
            PeopleUiState(contacts = contacts, groups = groups)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUiState())

    private val _expandedGroupId = MutableStateFlow<Long?>(null)
    val expandedGroupId: StateFlow<Long?> = _expandedGroupId.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun toggleGroupExpanded(id: Long) {
        _expandedGroupId.value = if (_expandedGroupId.value == id) null else id
    }

    fun dismissNotice() {
        _notice.value = null
    }

    fun addContact(name: String, phone: String) {
        if (name.isBlank()) {
            _notice.value = "Enter a name"
            return
        }
        if (!PhoneNumber.isValid(phone)) {
            _notice.value = "Enter a 10-digit number with the area code"
            return
        }
        viewModelScope.launch {
            val id = contactRepository.add(name, phone)
            _notice.value = if (id == null) "That number is already saved" else null
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch { contactRepository.update(contact) }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch { contactRepository.delete(id) }
    }

    fun addGroup(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { groupRepository.add(name) }
    }

    fun renameGroup(id: Long, name: String) {
        viewModelScope.launch { groupRepository.rename(id, name) }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch { groupRepository.delete(id) }
        if (_expandedGroupId.value == id) _expandedGroupId.value = null
    }

    fun setMembership(groupId: Long, contactId: Long, member: Boolean) {
        viewModelScope.launch { groupRepository.setMembership(groupId, contactId, member) }
    }

    companion object {
        fun factory() = containerViewModelFactory { container: AppContainer ->
            PeopleViewModel(container.contactRepository, container.groupRepository)
        }
    }
}
