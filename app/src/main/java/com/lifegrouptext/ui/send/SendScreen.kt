package com.lifegrouptext.ui.send

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifegrouptext.domain.previewOf
import com.lifegrouptext.ui.components.CenteredNote
import com.lifegrouptext.ui.components.Panel
import com.lifegrouptext.ui.components.PrimaryButton
import com.lifegrouptext.ui.components.ScreenHeader
import com.lifegrouptext.ui.components.SecondaryButton
import com.lifegrouptext.ui.components.SectionLabel
import com.lifegrouptext.ui.components.SelectablePill
import com.lifegrouptext.ui.components.countOf
import com.lifegrouptext.ui.theme.Ash
import com.lifegrouptext.ui.theme.Ink
import com.lifegrouptext.ui.theme.Slate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendScreen(
    onEditMessage: () -> Unit,
    viewModel: SendViewModel = viewModel(factory = SendViewModel.factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    val requestSms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.send() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Send")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (state.contacts.isEmpty()) {
                CenteredNote("Add some people first, over on the People tab.")
                return@Column
            }

            if (state.groups.isNotEmpty()) {
                SectionLabel("Groups")
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.groups.forEach { group ->
                        SelectablePill(
                            text = "${group.name} (${group.memberIds.size})",
                            selected = group.id in state.selectedGroupIds,
                            onClick = { viewModel.toggleGroup(group.id) },
                        )
                    }
                }
            }

            SectionLabel("People")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.contacts.forEach { contact ->
                    SelectablePill(
                        text = contact.firstName,
                        selected = contact.id in state.selectedContactIds,
                        onClick = { viewModel.toggleContact(contact.id) },
                    )
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryButton(text = "Select everyone", onClick = viewModel::selectAll)
                if (state.recipients.isNotEmpty()) {
                    SecondaryButton(text = "Clear", onClick = viewModel::clearSelection)
                }
            }

            SectionLabel("Message")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.hasMessage) {
                    Panel {
                        Text(
                            text = previewOf(state.body, state.recipients.firstOrNull()?.firstName),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink,
                        )
                    }
                } else {
                    Text(
                        text = "No message written yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ash,
                    )
                }
                SecondaryButton(
                    text = if (state.hasMessage) "Edit message" else "Write a message",
                    onClick = onEditMessage,
                )
            }
        }

        SendFooter(
            state = state,
            progress = progress,
            summary = summary,
            onSend = {
                if (viewModel.hasSmsPermission()) {
                    viewModel.send()
                } else {
                    requestSms.launch(Manifest.permission.SEND_SMS)
                }
            },
            onDismissSummary = viewModel::dismissSummary,
        )
    }
}

@Composable
private fun SendFooter(
    state: SendUiState,
    progress: com.lifegrouptext.sms.SendProgress?,
    summary: com.lifegrouptext.sms.SendSummary?,
    onSend: () -> Unit,
    onDismissSummary: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            progress != null -> {
                Text(
                    text = "Sending ${progress.completed + 1} of ${progress.total}" +
                        if (progress.currentName.isNotBlank()) " — ${progress.currentName}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate,
                )
            }

            summary != null -> {
                Text(
                    text = buildString {
                        append(countOf(summary.sent, "message"))
                        append(" sent")
                        if (summary.failed > 0) append(", ${summary.failed} failed — see the Log tab")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

        }

        PrimaryButton(
            text = when {
                progress != null -> "Sending…"
                !state.hasMessage -> "Write a message first"
                state.recipients.isEmpty() -> "Choose who to text"
                else -> "Send to ${countOf(state.recipients.size, "person", "people")}"
            },
            onClick = {
                onDismissSummary()
                onSend()
            },
            enabled = state.canSend && progress == null,
        )
    }
}
