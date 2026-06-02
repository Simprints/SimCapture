package org.dhis2.simprints.ramp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.simprints.ramp.data.GetEventHistoryTableUseCase

class EventHistoryTableViewModelFactory(
    private val getEventHistoryTable: GetEventHistoryTableUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        EventHistoryTableViewModel(
            getEventHistoryTable = getEventHistoryTable,
            dispatcherProvider = dispatcherProvider,
        ) as T
}
