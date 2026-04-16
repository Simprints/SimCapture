package org.dhis2.simprints.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolveConfirmIdentityCalloutUseCase
import org.dhis2.simprints.SimprintsSearchViewModel

class SimprintsSearchViewModelFactory(
    private val resolveConfirmIdentityCallout: SimprintsResolveConfirmIdentityCalloutUseCase,
    private val sessionRepository: SimprintsSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SimprintsSearchViewModel(
            resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
            sessionRepository = sessionRepository,
        ) as T
}
