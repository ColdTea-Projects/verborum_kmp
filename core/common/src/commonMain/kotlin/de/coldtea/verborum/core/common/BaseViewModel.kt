package de.coldtea.verborum.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI-flavoured base class for feature view models: one [StateFlow] of screen
 * state and one hot [SharedFlow] of one-shot effects (navigation, snackbars).
 */
abstract class BaseViewModel<State, Effect>(initialState: State) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 8)
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()

    protected val currentState: State get() = _state.value

    protected fun setState(reducer: State.() -> State) {
        _state.update(reducer)
    }

    protected fun emitEffect(effect: Effect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
