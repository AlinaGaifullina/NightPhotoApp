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
    object OnIsSaveSeriesChanged : SettingsEvent
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
            SettingsEvent.OnIsSaveSeriesChanged -> onIsSaveSeriesChanged()
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

    private fun onIsSaveSeriesChanged() {
        val isSaveSeries = _state.value.isSaveSeries
        _state.tryEmit(
            _state.value.copy(
                isSaveSeries = isSaveSeries
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