package org.dhis2.simprints.ramp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.simprints.ramp.data.GetEventHistoryTableUseCase
import org.dhis2.simprints.ramp.model.EventHistoryTableUiState

class EventHistoryTableViewModel(
    private val getEventHistoryTable: GetEventHistoryTableUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow<EventHistoryTableUiState>(EventHistoryTableUiState.Loading)
    val uiState: StateFlow<EventHistoryTableUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(dispatcherProvider.io()) {
            if (_uiState.value !is EventHistoryTableUiState.Success) {
                _uiState.value = EventHistoryTableUiState.Loading
            }
            _uiState.value =
                getEventHistoryTable()
                    .fold(
                        onSuccess = { table ->
                            table
                                ?.let(EventHistoryTableUiState::Success)
                                ?: EventHistoryTableUiState.Empty
                        },
                        onFailure = { error ->
                            EventHistoryTableUiState.Error(error.message)
                        },
                    )
        }
    }
}
