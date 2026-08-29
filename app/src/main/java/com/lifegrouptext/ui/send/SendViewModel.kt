package com.lifegrouptext.ui.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifegrouptext.data.ContactRepository
import com.lifegrouptext.data.DraftRepository
import com.lifegrouptext.data.GroupRepository
import com.lifegrouptext.di.AppContainer
import com.lifegrouptext.domain.Contact
import com.lifegrouptext.domain.Group
import com.lifegrouptext.sms.BulkSender
import com.lifegrouptext.sms.SendProgress
import com.lifegrouptext.sms.SendSummary
import com.lifegrouptext.sms.SmsMetrics
import com.lifegrouptext.sms.SmsSender
import com.lifegrouptext.sms.SmsText
import com.lifegrouptext.ui.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SendUiState(
    val contacts: List<Contact> = emptyList(),
    val groups: List<Group> = emptyList(),
    val selectedContactIds: Set<Long> = emptySet(),
    val selectedGroupIds: Set<Long> = emptySet(),
    val body: String = "",
    val metrics: SmsMetrics = SmsText.measure(""),
) {
    /** Everyone the message will go to: hand-picked people plus every selected group's members. */
    val recipients: List<Contact>
        get() {
            if (selectedContactIds.isEmpty() && selectedGroupIds.isEmpty()) return emptyList()
            val fromGroups = groups
                .filter { it.id in selectedGroupIds }
                .flatMap { it.memberIds }
                .toSet()
            val ids = selectedContactIds + fromGroups
            return contacts.filter { it.id in ids }
        }

    val hasMessage: Boolean get() = body.isNotBlank()
    val canSend: Boolean get() = hasMessage && recipients.isNotEmpty()

    /** Segments per recipient multiplied out — what the carrier will actually bill. */
    val totalTexts: Int get() = metrics.segments * recipients.size
}

class SendViewModel(
    contactRepository: ContactRepository,
    groupRepository: GroupRepository,
    draftRepository: DraftRepository,
    private val bulkSender: BulkSender,
    private val smsSender: SmsSender,
) : ViewModel() {

    private val selectedContactIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedGroupIds = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<SendUiState> = combine(
        contactRepository.observeAll(),
        groupRepository.observeAll(),
        draftRepository.observe(),
        selectedContactIds,
        selectedGroupIds,
    ) { contacts, groups, body, pickedContacts, pickedGroups ->
        // Drop selections whose contact or group has since been deleted.
        val liveContactIds = contacts.map { it.id }.toSet()
        val liveGroupIds = groups.map { it.id }.toSet()
        SendUiState(
            contacts = contacts,
            groups = groups,
            selectedContactIds = pickedContacts intersect liveContactIds,
            selectedGroupIds = pickedGroups intersect liveGroupIds,
            body = body,
            metrics = SmsText.measure(SmsText.sanitize(body)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SendUiState())

    private val _progress = MutableStateFlow<SendProgress?>(null)
    val progress: StateFlow<SendProgress?> = _progress.asStateFlow()

    private val _summary = MutableStateFlow<SendSummary?>(null)
    val summary: StateFlow<SendSummary?> = _summary.asStateFlow()

    fun toggleContact(id: Long) {
        selectedContactIds.value = selectedContactIds.value.toggle(id)
    }

    fun toggleGroup(id: Long) {
        selectedGroupIds.value = selectedGroupIds.value.toggle(id)
    }

    fun selectAll() {
        selectedContactIds.value = state.value.contacts.map { it.id }.toSet()
        selectedGroupIds.value = emptySet()
    }

    fun clearSelection() {
        selectedContactIds.value = emptySet()
        selectedGroupIds.value = emptySet()
    }

    fun dismissSummary() {
        _summary.value = null
    }

    fun hasSmsPermission(): Boolean = smsSender.hasPermission()

    fun send() {
        val current = state.value
        if (!current.canSend || _progress.value != null) return

        viewModelScope.launch {
            _summary.value = null
            val result = bulkSender.send(
                recipients = current.recipients,
                body = current.body,
                onProgress = { _progress.value = it },
            )
            _progress.value = null
            _summary.value = result
        }
    }

    private fun Set<Long>.toggle(id: Long): Set<Long> =
        if (id in this) this - id else this + id

    companion object {
        fun factory() = containerViewModelFactory { container: AppContainer ->
            SendViewModel(
                container.contactRepository,
                container.groupRepository,
                container.draftRepository,
                container.bulkSender,
                container.smsSender,
            )
        }
    }
}
