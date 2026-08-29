package com.lifegrouptext.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifegrouptext.data.ContactRepository
import com.lifegrouptext.di.AppContainer
import com.lifegrouptext.domain.PhoneContact
import com.lifegrouptext.domain.PhoneNumber
import com.lifegrouptext.domain.contactMatches
import com.lifegrouptext.ui.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ImportUiState(
    val loading: Boolean = false,
    val permissionDenied: Boolean = false,
    val query: String = "",
    val results: List<ImportRow> = emptyList(),
)

data class ImportRow(
    val contact: PhoneContact,
    val alreadySaved: Boolean,
)

class ImportContactsViewModel(
    private val contactRepository: ContactRepository,
) : ViewModel() {

    private val phoneContacts = MutableStateFlow<List<PhoneContact>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)
    private val permissionDenied = MutableStateFlow(false)

    private val savedNumbers = contactRepository.observeAll()
        .map { saved -> saved.map { it.phone } }

    val state: StateFlow<ImportUiState> =
        combine(phoneContacts, query, savedNumbers, loading, permissionDenied) {
                found, text, saved, isLoading, denied ->
            val filtered = found.filter { candidate ->
                contactMatches(candidate.name, candidate.phone, text)
            }
            ImportUiState(
                loading = isLoading,
                permissionDenied = denied,
                query = text,
                results = filtered.map { candidate ->
                    ImportRow(
                        contact = candidate,
                        alreadySaved = saved.any { PhoneNumber.sameNumber(it, candidate.phone) },
                    )
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImportUiState())

    private val _justAdded = MutableStateFlow<String?>(null)
    val justAdded: StateFlow<String?> = _justAdded.asStateFlow()

    /** Called once the caller knows whether READ_CONTACTS was granted. */
    fun load(granted: Boolean) {
        permissionDenied.value = !granted
        if (!granted) {
            phoneContacts.value = emptyList()
            return
        }
        viewModelScope.launch {
            loading.value = true
            phoneContacts.value = contactRepository.readPhoneContacts()
            loading.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun import(row: ImportRow) {
        if (row.alreadySaved) return
        viewModelScope.launch {
            contactRepository.add(row.contact.name, row.contact.phone)
            _justAdded.value = row.contact.name
        }
    }

    companion object {
        fun factory() = containerViewModelFactory { container: AppContainer ->
            ImportContactsViewModel(container.contactRepository)
        }
    }
}
