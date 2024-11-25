package ru.itis.nightphotoapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val path: String = "",
    val isSaveSeries: Boolean = false,
)

sealed interface SettingsSideEffect {
    object NavigateBack : SettingsSideEffect
}

sealed interface SettingsEvent {
    data class OnCheckboxClick(val status: Boolean) : SettingsEvent
    object OnBackButtonClick : SettingsEvent
    data class OnPathChanged(val path: String) : SettingsEvent
}
class SettingsViewModel(

) : ViewModel() {

    private val _state: MutableStateFlow<SettingsState> = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    private val _action = MutableSharedFlow<SettingsSideEffect?>()
    val action: SharedFlow<SettingsSideEffect?>
        get() = _action.asSharedFlow()

    fun event(settingsEvent: SettingsEvent) {
        when (settingsEvent) {
            is SettingsEvent.OnCheckboxClick -> onCheckboxClick(settingsEvent.status)
            SettingsEvent.OnBackButtonClick -> onBackButtonClick()
            is SettingsEvent.OnPathChanged -> onPathChanged(settingsEvent.path)
        }
    }


    private fun onBackButtonClick() {
        viewModelScope.launch {
            _action.emit(
                SettingsSideEffect.NavigateBack
            )
        }
    }

    private fun onCheckboxClick(status: Boolean) {
        _state.tryEmit(
            _state.value.copy(
                isSaveSeries = !status
            )
        )
    }

    private fun onPathChanged(path: String) {
        _state.tryEmit(
            _state.value.copy(
                path = path
            )
        )
    }
}