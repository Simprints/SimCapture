package org.dhis2.simprints.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolvePendingEnrollmentActionUseCase
import org.dhis2.simprints.SimprintsCustomIntentResultMapper
import org.dhis2.simprints.SimprintsEnrollmentViewModel

class SimprintsEnrollmentViewModelFactory(
    private val simprintsD2Repository: SimprintsD2Repository,
    private val resolvePendingEnrollmentAction: SimprintsResolvePendingEnrollmentActionUseCase,
    private val sessionRepository: SimprintsSessionRepository,
    private val resultMapper: SimprintsCustomIntentResultMapper,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SimprintsEnrollmentViewModel(
            simprintsD2Repository = simprintsD2Repository,
            resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
            sessionRepository = sessionRepository,
            resultMapper = resultMapper,
        ) as T
}
