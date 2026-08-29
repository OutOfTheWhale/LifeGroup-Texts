package com.lifegrouptext.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifegrouptext.data.SendLogRepository
import com.lifegrouptext.di.AppContainer
import com.lifegrouptext.domain.SendLogEntry
import com.lifegrouptext.ui.containerViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(
    private val repository: SendLogRepository,
) : ViewModel() {

    val entries: StateFlow<List<SendLogEntry>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }

    companion object {
        fun factory() = containerViewModelFactory { container: AppContainer ->
            LogViewModel(container.sendLogRepository)
        }
    }
}
