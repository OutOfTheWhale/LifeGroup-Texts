package com.lifegrouptext.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifegrouptext.domain.SendLogEntry
import com.lifegrouptext.domain.SendStatus
import com.lifegrouptext.ui.components.CenteredNote
import com.lifegrouptext.ui.components.RowDivider
import com.lifegrouptext.ui.components.ScreenHeader
import com.lifegrouptext.ui.components.SecondaryButton
import com.lifegrouptext.ui.components.countOf
import com.lifegrouptext.ui.components.formatPhone
import com.lifegrouptext.ui.components.formatTimestamp
import com.lifegrouptext.ui.theme.Ash
import com.lifegrouptext.ui.theme.Ink
import com.lifegrouptext.ui.theme.Slate

/**
 * What was actually sent. Every row reflects a real result from the radio, so a
 * failure shows as a failure — the old app logged "sent" unconditionally.
 */
@Composable
fun LogScreen(
    viewModel: LogViewModel = viewModel(factory = LogViewModel.factory()),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Log") {
            if (entries.isNotEmpty()) {
                SecondaryButton(text = "Clear", onClick = viewModel::clear)
            }
        }

        if (entries.isEmpty()) {
            CenteredNote("Nothing sent yet.")
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { it.id }) { entry ->
                LogRow(entry)
                RowDivider()
            }
        }
    }
}

@Composable
private fun LogRow(entry: SendLogEntry) {
    val failed = entry.status == SendStatus.FAILED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = entry.contactName,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
        )
        Text(
            text = buildString {
                append(formatPhone(entry.phone))
                append(" · ")
                append(formatTimestamp(entry.sentAt))
                if (entry.segments > 0) {
                    append(" · ")
                    append(countOf(entry.segments, "text"))
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Slate,
        )
        Text(
            text = entry.body,
            style = MaterialTheme.typography.bodyMedium,
            color = Ash,
        )
        Text(
            text = if (failed) "Failed — ${entry.failureReason ?: "unknown reason"}" else "Sent",
            style = MaterialTheme.typography.labelLarge,
            color = if (failed) Ink else Slate,
        )
    }
}
