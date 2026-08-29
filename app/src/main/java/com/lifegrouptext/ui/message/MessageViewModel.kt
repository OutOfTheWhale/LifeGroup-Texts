package com.lifegrouptext.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifegrouptext.data.ContactRepository
import com.lifegrouptext.data.DraftRepository
import com.lifegrouptext.di.AppContainer
import com.lifegrouptext.sms.SmsEncoding
import com.lifegrouptext.sms.SmsMetrics
import com.lifegrouptext.sms.SmsText
import com.lifegrouptext.ui.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MessageUiState(
    val body: String = "",
    val metrics: SmsMetrics = SmsText.measure(""),
    val offenders: List<String> = emptyList(),
    val sampleName: String = "Friend",
) {
    val isUcs2: Boolean get() = metrics.encoding == SmsEncoding.UCS2
    val isEmpty: Boolean get() = body.isBlank()
}

class MessageViewModel(
    private val draftRepository: DraftRepository,
    contactRepository: ContactRepository,
) : ViewModel() {

    private val body = MutableStateFlow<String?>(null)

    private val sampleName = contactRepository.observeAll()
        .map { it.firstOrNull()?.firstName ?: "Friend" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Friend")

    val state: StateFlow<MessageUiState> =
        combine(draftRepository.observe(), body, sampleName) { saved, edited, name ->
            // Until the user types, the saved draft is the source of truth.
            val text = edited ?: saved
            MessageUiState(
                body = text,
                metrics = SmsText.measure(text),
                offenders = SmsText.nonGsmCharacters(text),
                sampleName = name,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessageUiState())

    fun onBodyChange(value: String) {
        body.value = value
        viewModelScope.launch { draftRepository.save(value) }
    }

    /** Rewrite curly punctuation in place, so the message drops back to 160-per-segment. */
    fun tidyPunctuation() = onBodyChange(SmsText.sanitize(state.value.body))

    /** Also drop emoji and anything else outside the GSM alphabet. */
    fun removeSpecialCharacters() = onBodyChange(SmsText.stripNonGsm(state.value.body))

    fun insertNameToken() = onBodyChange(state.value.body + com.lifegrouptext.domain.NAME_TOKEN)

    companion object {
        fun factory() = containerViewModelFactory { container: AppContainer ->
            MessageViewModel(container.draftRepository, container.contactRepository)
        }
    }
}
