package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.composables.ExpandableWordCard
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.PracticeWordUi
import de.coldtea.verborum.core.localization.strings

/**
 * iOS: the Android app's design, deliberately unchanged — one column of expandable cards over a
 * sticky "switch sides" button. Full width rather than the app's usual capped column, because that
 * is how the phone screen it mirrors is laid out.
 */
@Composable
internal actual fun SelfPracticeContent(
    words: List<PracticeWordUi>,
    openWordIds: Set<String>,
    onToggleOpen: (String) -> Unit,
    onCorrect: (String) -> Unit,
    onWrong: (String) -> Unit,
    onSwitchSides: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.large),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            items(words, key = PracticeWordUi::wordId) { word ->
                ExpandableWordCard(
                    word = word,
                    isRevealed = word.wordId in openWordIds,
                    onToggleReveal = { onToggleOpen(word.wordId) },
                    onCorrect = { onCorrect(word.wordId) },
                    onWrong = { onWrong(word.wordId) },
                )
            }
        }

        Button(
            onClick = onSwitchSides,
            shape = Shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.medium)
                .height(Dimens.buttonHeight),
        ) {
            Text(text = strings.switchSides, style = MaterialTheme.typography.titleSmall)
        }
    }
}
