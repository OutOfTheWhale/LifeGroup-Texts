package com.lifegrouptext.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifegrouptext.domain.NAME_TOKEN
import com.lifegrouptext.domain.previewOf
import com.lifegrouptext.ui.components.Panel
import com.lifegrouptext.ui.components.trackEditing
import com.lifegrouptext.ui.components.ScreenHeader
import com.lifegrouptext.ui.components.SecondaryButton
import com.lifegrouptext.ui.components.SectionLabel
import com.lifegrouptext.ui.theme.Ash
import com.lifegrouptext.ui.theme.Hairline
import com.lifegrouptext.ui.theme.Ink
import com.lifegrouptext.ui.theme.Slate

/**
 * One message, any length. The old app split long text across numbered boxes because
 * only the first segment ever sent; the counter below reports segments honestly
 * instead, and the phone handles the splitting.
 */
@Composable
fun MessageScreen(
    viewModel: MessageViewModel = viewModel(factory = MessageViewModel.factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Message")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Type $NAME_TOKEN anywhere and it becomes each person's first name.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
            )

            BasicTextField(
                value = state.body,
                onValueChange = viewModel::onBodyChange,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyLarge.copy(color = Ink),
                ),
                cursorBrush = SolidColor(Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                    .padding(14.dp)
                    .trackEditing(),
                decorationBox = { inner ->
                    if (state.body.isEmpty()) {
                        Text(
                            text = "Hi $NAME_TOKEN, reminder that we meet Sunday at 10am.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ash,
                        )
                    }
                    inner()
                },
            )

            CharacterCount(state.body.length)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(text = "Insert $NAME_TOKEN", onClick = viewModel::insertNameToken)
            }


            if (!state.isEmpty) {
                SectionLabel("Preview", Modifier.padding(horizontal = 0.dp))
                Panel {
                    Text(
                        text = previewOf(state.body, state.sampleName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink,
                    )
                }
            }
        }
    }
}

/** Just how long the message is. The phone handles splitting and the recipient
 * sees one message either way, so segment counts would only be noise. */
@Composable
private fun CharacterCount(characters: Int) {
    Text(
        text = "$characters characters",
        style = MaterialTheme.typography.bodyMedium,
        color = Slate,
        modifier = Modifier.fillMaxWidth(),
    )
}

