package com.lifegrouptext.ui.people

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifegrouptext.ui.components.BackHeader
import com.lifegrouptext.ui.components.CenteredNote
import com.lifegrouptext.ui.components.LightTextField
import com.lifegrouptext.ui.components.ListRow
import com.lifegrouptext.ui.components.LocalEditingTracker
import com.lifegrouptext.ui.components.RowDivider
import com.lifegrouptext.ui.components.formatPhone
import com.lifegrouptext.ui.theme.Ash
import com.lifegrouptext.ui.theme.Slate

/** Pick people out of the phone's own contact list. */
@Composable
fun ImportContactsScreen(
    onBack: () -> Unit,
    viewModel: ImportContactsViewModel = viewModel(factory = ImportContactsViewModel.factory()),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    // With the keyboard up there is barely any room left on an LP3, and the results
    // are the whole point of this screen — so the title bar steps aside while typing.
    val editing = LocalEditingTracker.current.isEditing

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.load(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) viewModel.load(true) else requestPermission.launch(Manifest.permission.READ_CONTACTS)
    }

    Column(Modifier.fillMaxSize()) {
        if (!editing) BackHeader("Import contacts", onBack)

        LightTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search your contacts",
            imeAction = ImeAction.Search,
            onSubmit = { keyboard?.hide() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (editing) 6.dp else 12.dp),
        )

        Box(Modifier.weight(1f)) {
        when {
            state.permissionDenied -> CenteredNote(
                "Life Group Texts needs permission to read your contacts. " +
                    "You can grant it in Android Settings, or add people by hand instead.",
            )

            state.loading -> CenteredNote("Reading your contacts…")

            state.results.isEmpty() -> CenteredNote(
                if (state.query.isBlank()) "No contacts on this phone." else "Nothing matches that.",
            )

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.results, key = { it.contact.phone }) { row ->
                    ListRow(
                        title = row.contact.name,
                        subtitle = formatPhone(row.contact.phone),
                        onClick = {
                            keyboard?.hide()
                            viewModel.import(row)
                        },
                        trailing = {
                            Text(
                                text = if (row.alreadySaved) "Added" else "Add",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (row.alreadySaved) Ash else Slate,
                                modifier = Modifier.padding(8.dp),
                            )
                        },
                    )
                    RowDivider()
                }
            }
        }
        }
    }
}
