package com.lifegrouptext.ui.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifegrouptext.domain.Contact
import com.lifegrouptext.domain.Group
import com.lifegrouptext.ui.components.CheckMark
import com.lifegrouptext.ui.components.LightTextField
import com.lifegrouptext.ui.components.ListRow
import com.lifegrouptext.ui.components.RowDivider
import com.lifegrouptext.ui.components.ScreenHeader
import com.lifegrouptext.ui.components.SecondaryButton
import com.lifegrouptext.ui.components.SectionLabel
import com.lifegrouptext.ui.components.countOf
import com.lifegrouptext.ui.components.formatPhone
import com.lifegrouptext.ui.theme.Ash
import com.lifegrouptext.ui.theme.Ink
import com.lifegrouptext.ui.theme.Slate

@Composable
fun PeopleScreen(
    onImport: () -> Unit,
    viewModel: PeopleViewModel = viewModel(factory = PeopleViewModel.factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expandedGroupId by viewModel.expandedGroupId.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    var addingContact by remember { mutableStateOf(false) }
    var addingGroup by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Contact?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("People")

        LazyColumn(Modifier.fillMaxSize()) {
            if (notice != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = viewModel::dismissNotice)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = notice.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                        )
                    }
                }
            }

            item {
                SectionHeaderRow(
                    label = "Groups",
                    actionLabel = if (addingGroup) "Cancel" else "New group",
                    onAction = { addingGroup = !addingGroup },
                )
            }

            if (addingGroup) {
                item {
                    NameEntry(
                        placeholder = "Group name",
                        onSubmit = { name ->
                            viewModel.addGroup(name)
                            addingGroup = false
                        },
                    )
                }
            }

            if (state.groups.isEmpty()) {
                item { EmptyLine("No groups yet.") }
            }

            items(state.groups, key = { it.id }) { group ->
                GroupRow(
                    group = group,
                    contacts = state.contacts,
                    expanded = expandedGroupId == group.id,
                    onToggle = { viewModel.toggleGroupExpanded(group.id) },
                    onRename = { viewModel.renameGroup(group.id, it) },
                    onDelete = { viewModel.deleteGroup(group.id) },
                    onSetMembership = { contactId, member ->
                        viewModel.setMembership(group.id, contactId, member)
                    },
                )
                RowDivider()
            }

            item {
                SectionHeaderRow(
                    label = "Contacts (${state.contacts.size})",
                    actionLabel = if (addingContact) "Cancel" else "Add",
                    onAction = { addingContact = !addingContact },
                    secondaryLabel = "Import",
                    onSecondary = onImport,
                )
            }

            if (addingContact) {
                item {
                    ContactEntry(
                        onSubmit = { name, phone ->
                            viewModel.addContact(name, phone)
                            addingContact = false
                        },
                    )
                }
            }

            if (state.contacts.isEmpty()) {
                item { EmptyLine("No contacts yet. Add one, or import from your phone.") }
            }

            items(state.contacts, key = { it.id }) { contact ->
                if (editing?.id == contact.id) {
                    ContactEntry(
                        initialName = contact.name,
                        initialPhone = contact.phone,
                        onSubmit = { name, phone ->
                            viewModel.updateContact(contact.copy(name = name, phone = phone))
                            editing = null
                        },
                        onCancel = { editing = null },
                    )
                } else {
                    ListRow(
                        title = contact.name,
                        subtitle = formatPhone(contact.phone),
                        onClick = { editing = contact },
                        trailing = {
                            Text(
                                text = "Remove",
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate,
                                modifier = Modifier
                                    .clickable { viewModel.deleteContact(contact.id) }
                                    .padding(8.dp),
                            )
                        },
                    )
                }
                RowDivider()
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    label: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(label, Modifier.weight(1f))
        if (secondaryLabel != null && onSecondary != null) {
            SecondaryButton(text = secondaryLabel, onClick = onSecondary)
            Row(Modifier.padding(horizontal = 4.dp)) {}
        }
        SecondaryButton(text = actionLabel, onClick = onAction)
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Ash,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun NameEntry(placeholder: String, onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LightTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = placeholder,
            imeAction = ImeAction.Done,
            onSubmit = { onSubmit(value) },
        )
        SecondaryButton(text = "Create", onClick = { onSubmit(value) })
    }
}

@Composable
private fun ContactEntry(
    initialName: String = "",
    initialPhone: String = "",
    onSubmit: (String, String) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LightTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Full name",
            imeAction = ImeAction.Next,
        )
        LightTextField(
            value = phone,
            onValueChange = { phone = it },
            placeholder = "Phone with area code",
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
            onSubmit = { onSubmit(name, phone) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(text = "Save", onClick = { onSubmit(name, phone) })
            if (onCancel != null) SecondaryButton(text = "Cancel", onClick = onCancel)
        }
    }
}

@Composable
private fun GroupRow(
    group: Group,
    contacts: List<Contact>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onSetMembership: (Long, Boolean) -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }

    ListRow(
        title = group.name,
        subtitle = countOf(group.memberIds.size, "member"),
        onClick = onToggle,
        endText = if (expanded) "−" else "+",
    )

    if (expanded) {
        Column(Modifier.padding(bottom = 8.dp)) {
            if (renaming) {
                NameEntry(
                    placeholder = "Rename group",
                    onSubmit = {
                        onRename(it)
                        renaming = false
                    },
                )
            } else {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SecondaryButton(text = "Rename", onClick = { renaming = true })
                    SecondaryButton(text = "Delete group", onClick = onDelete)
                }
            }

            if (contacts.isEmpty()) {
                EmptyLine("Add contacts first, then pick who belongs here.")
            } else {
                SectionLabel("Tap to add or remove")
                contacts.forEach { contact ->
                    val member = contact.id in group.memberIds
                    ListRow(
                        title = contact.name,
                        subtitle = formatPhone(contact.phone),
                        onClick = { onSetMembership(contact.id, !member) },
                        leading = { CheckMark(member) },
                    )
                }
            }
        }
    }
}
