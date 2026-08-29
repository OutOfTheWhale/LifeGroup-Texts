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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifegrouptext.domain.NAME_TOKEN
import com.lifegrouptext.domain.previewOf
import com.lifegrouptext.sms.SmsMetrics
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

            SegmentCounter(state.metrics)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(text = "Insert $NAME_TOKEN", onClick = viewModel::insertNameToken)
            }

            if (state.isUcs2) {
                SpecialCharacterNotice(
                    offenders = state.offenders,
                    onTidy = viewModel::tidyPunctuation,
                    onStrip = viewModel::removeSpecialCharacters,
                )
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

/**
 * Reports what the message actually costs. Characters alone are misleading, because
 * the per-segment allowance depends on the alphabet in use.
 */
@Composable
private fun SegmentCounter(metrics: SmsMetrics) {
    val unitWord = if (metrics.encoding == com.lifegrouptext.sms.SmsEncoding.UCS2) {
        "characters (Unicode)"
    } else {
        "characters"
    }
    val segmentText = when (metrics.segments) {
        0 -> "empty"
        1 -> "1 text"
        else -> "${metrics.segments} texts"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${metrics.units} $unitWord",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate,
        )
        Text(
            text = if (metrics.segments > 1) {
                "$segmentText · ${metrics.remaining} left in this one"
            } else {
                "$segmentText · ${metrics.remaining} left"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (metrics.segments > 1) Ink else Slate,
        )
    }
}

/**
 * Shown only when something has pushed the message to UCS-2. This is the failure the
 * old app hit silently, so it is stated plainly along with the two ways out.
 */
@Composable
private fun SpecialCharacterNotice(
    offenders: List<String>,
    onTidy: () -> Unit,
    onStrip: () -> Unit,
) {
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Special characters are shortening your texts",
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
            Text(
                text = "These characters can't be sent in the plain SMS alphabet, so each " +
                    "text now holds 70 characters instead of 160: " +
                    offenders.take(12).joinToString(" "),
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
            )
            Text(
                text = "The message will still send correctly — it just costs more texts.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ash,
                fontStyle = FontStyle.Italic,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(text = "Fix punctuation", onClick = onTidy)
                SecondaryButton(text = "Remove them", onClick = onStrip)
            }
        }
    }
}
