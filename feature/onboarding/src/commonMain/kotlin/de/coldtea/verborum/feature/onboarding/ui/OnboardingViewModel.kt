package de.coldtea.verborum.feature.onboarding.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.feature.onboarding.domain.OnboardingService
import kotlinx.coroutines.launch

/** Whether the tour still has to be shown unprompted; null until the answer has been read. */
data class OnboardingUiState(val isCompleted: Boolean? = null)

class OnboardingViewModel(
    private val onboardingService: OnboardingService,
) : BaseViewModel<OnboardingUiState, Nothing>(OnboardingUiState()) {

    init {
        viewModelScope.launch {
            val isCompleted = onboardingService.isCompleted()

            setState { copy(isCompleted = isCompleted) }
        }
    }

    /**
     * Marks the tour as seen. Recorded before the caller navigates away, so closing it is what makes
     * it stop appearing — there is no second confirmation step to miss.
     */
    fun complete() {
        setState { copy(isCompleted = true) }

        viewModelScope.launch { onboardingService.complete() }
    }
}
