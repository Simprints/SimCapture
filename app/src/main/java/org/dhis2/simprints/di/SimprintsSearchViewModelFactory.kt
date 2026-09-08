package org.dhis2.simprints.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolveConfirmIdentityCalloutUseCase
import org.dhis2.simprints.SimprintsResolveSingleBiometricSearchNavigationUseCase
import org.dhis2.simprints.SimprintsSearchViewModel

class SimprintsSearchViewModelFactory(
    private val resolveConfirmIdentityCallout: SimprintsResolveConfirmIdentityCalloutUseCase,
    private val sessionRepository: SimprintsSessionRepository,
    private val resolveSingleBiometricSearchNavigation: SimprintsResolveSingleBiometricSearchNavigationUseCase,
    private val simprintsD2Repository: SimprintsD2Repository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SimprintsSearchViewModel(
            resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
            sessionRepository = sessionRepository,
            resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            simprintsD2Repository = simprintsD2Repository,
        ) as T
}
