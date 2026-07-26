package de.coldtea.verborum.feature.bibliotheca.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.data.Word
import de.coldtea.verborum.feature.bibliotheca.data.WordRepository
import org.koin.compose.koinInject

@Composable
fun WordDetailScreen(
    wordId: String,
    modifier: Modifier = Modifier,
    repository: WordRepository = koinInject(),
) {
    var outcome: Outcome<Word> by remember(wordId) { mutableStateOf(Outcome.Loading) }

    LaunchedEffect(wordId) {
        outcome = repository.word(wordId)
    }

    when (val current = outcome) {
        Outcome.Loading -> LoadingState(modifier)
        is Outcome.Failure -> ErrorState("That entry could not be found.", modifier)
        is Outcome.Success -> Column(
            modifier = modifier.fillMaxSize().padding(Spacing.large),
        ) {
            Text(current.data.lemma, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = current.data.translation,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = Spacing.extraSmall),
            )
            Text(
                text = current.data.definition,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = Spacing.medium),
            )
        }
    }
}
