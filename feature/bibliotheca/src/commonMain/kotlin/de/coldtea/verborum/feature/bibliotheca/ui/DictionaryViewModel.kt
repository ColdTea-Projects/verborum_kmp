package de.coldtea.verborum.feature.bibliotheca.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.data.Word
import de.coldtea.verborum.feature.bibliotheca.data.WordRepository
import kotlinx.coroutines.launch

data class DictionaryState(
    val query: String = "",
    val words: List<Word> = emptyList(),
    val isLoading: Boolean = true,
    val error: VerborumError? = null,
)

sealed interface DictionaryEffect {
    data class OpenWord(val id: String) : DictionaryEffect
}

class DictionaryViewModel(
    private val repository: WordRepository,
) : BaseViewModel<DictionaryState, DictionaryEffect>(DictionaryState()) {

    init {
        search(query = "")
    }

    fun search(query: String) {
        setState { copy(query = query, isLoading = true, error = null) }

        viewModelScope.launch {
            when (val outcome = repository.search(query)) {
                is Outcome.Success -> setState { copy(words = outcome.data, isLoading = false) }
                is Outcome.Failure -> setState { copy(isLoading = false, error = outcome.error) }
                Outcome.Loading -> setState { copy(isLoading = true) }
            }
        }
    }

    fun onWordClicked(id: String) = emitEffect(DictionaryEffect.OpenWord(id))

    fun retry() = search(currentState.query)
}
