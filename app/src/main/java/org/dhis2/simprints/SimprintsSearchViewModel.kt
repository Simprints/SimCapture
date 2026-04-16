package org.dhis2.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolveConfirmIdentityCalloutUseCase
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils

@OptIn(ExperimentalAtomicApi::class)
class SimprintsSearchViewModel(
    private val resolveConfirmIdentityCallout: SimprintsResolveConfirmIdentityCalloutUseCase,
    private val sessionRepository: SimprintsSessionRepository,
) : ViewModel() {
    data class PendingDashboardNavigation(
        val teiUid: String,
        val programUid: String?,
        val enrollmentUid: String?,
    )

    sealed class DashboardAction {
        data class LaunchConfirmIdentity(
            val intent: Intent,
        ) : DashboardAction()

        data class OpenDashboard(
            val navigation: PendingDashboardNavigation,
        ) : DashboardAction()
    }

    private val pendingDashboardNavigation = AtomicReference<PendingDashboardNavigation?>(null)

    suspend fun onDashboardRequested(
        searchFields: List<SimprintsSearchUtils.SearchField>,
        teiUid: String,
        programUid: String?,
        enrollmentUid: String?,
    ): DashboardAction {
        val searchState = SimprintsSearchUtils.searchState(searchFields)
        val sessionId =
            sessionRepository.get()?.takeIf { searchState.hasBiometricIdentificationQuery }
        val confirmIdentityIntent =
            sessionId?.let {
                resolveConfirmIdentityCallout(
                    teiUid = teiUid,
                    searchFields = searchFields,
                    sessionId = it,
                )?.launchIntent
            }

        val navigation =
            PendingDashboardNavigation(
                teiUid = teiUid,
                programUid = programUid,
                enrollmentUid = enrollmentUid,
            )
        if (confirmIdentityIntent == null) {
            return DashboardAction.OpenDashboard(navigation)
        }

        pendingDashboardNavigation.store(navigation)
        sessionRepository.clear()
        return DashboardAction.LaunchConfirmIdentity(confirmIdentityIntent)
    }

    fun prepareEnrollmentQueryData(
        searchFields: List<SimprintsSearchUtils.SearchField>,
        queryData: Map<String, List<String>?>,
    ): HashMap<String, List<String>> {
        val searchState = SimprintsSearchUtils.searchState(searchFields)

        if (searchState.hasBiometricIdentificationQuery && sessionRepository.hasPendingSession()) {
            sessionRepository.markPendingEnrollment()
        }

        return SimprintsSearchUtils.filterQueryData(
            queryData = queryData,
            fields = searchFields,
        )
    }

    fun onConfirmIdentityResult(resultCode: Int): PendingDashboardNavigation? =
        pendingDashboardNavigation.exchange(null)
            ?.takeIf { resultCode == RESULT_OK }

    fun onConfirmIdentityLaunchFailed() {
        pendingDashboardNavigation.store(null)
    }

    fun clearPendingSessionIfNeeded(searchFields: List<SimprintsSearchUtils.SearchField>) {
        val searchState = SimprintsSearchUtils.searchState(searchFields)
        if (sessionRepository.hasPendingSession() && searchState.shouldClearPendingSession) {
            sessionRepository.clear()
        }
    }

    fun shouldUseLastBiometricsLabel(searchFields: List<SimprintsSearchUtils.SearchField>): Boolean {
        val searchState = SimprintsSearchUtils.searchState(searchFields)
        return SimprintsSearchUtils.shouldUseLastBiometricsLabel(
            searchState = searchState,
            hasPendingSession = sessionRepository.hasPendingSession(),
        )
    }
}
