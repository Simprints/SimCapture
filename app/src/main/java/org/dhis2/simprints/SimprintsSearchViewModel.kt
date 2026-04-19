package org.dhis2.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolveConfirmIdentityCalloutUseCase
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.model.FieldUiModelImpl
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class SimprintsSearchViewModel(
    private val resolveConfirmIdentityCallout: SimprintsResolveConfirmIdentityCalloutUseCase,
    private val sessionRepository: SimprintsSessionRepository,
    private val resolveSingleBiometricSearchNavigation: SimprintsResolveSingleBiometricSearchNavigationUseCase,
) : ViewModel() {
    private data class PendingSimprintsMfidBiometricIdentification(
        val uid: String,
        val value: String?,
    )

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
    private var pendingSimprintsMfidBiometricIdentification: PendingSimprintsMfidBiometricIdentification? =
        null
    private val _simprintsBiometricSearchNavigation = Channel<Unit>(Channel.BUFFERED)
    val simprintsBiometricSearchNavigation: Flow<Unit> =
        _simprintsBiometricSearchNavigation.receiveAsFlow()
    private val _isSimprintsBiometricSearch = MutableLiveData(false)
    val isSimprintsBiometricSearch: LiveData<Boolean> = _isSimprintsBiometricSearch
    private val _isSimprintsUseLastBiometricsLabel = MutableLiveData(false)
    val isSimprintsUseLastBiometricsLabel: LiveData<Boolean> = _isSimprintsUseLastBiometricsLabel

    suspend fun onDashboardRequested(
        searchItems: List<FieldUiModel>,
        teiUid: String,
        programUid: String?,
        enrollmentUid: String?,
    ): DashboardAction {
        val searchFields = searchItems.toSearchFields()
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
        searchItems: List<FieldUiModel>,
        queryData: Map<String, List<String>?>,
    ): HashMap<String, List<String>> {
        val searchFields = searchItems.toSearchFields()
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

    fun clearPendingSession() {
        pendingDashboardNavigation.store(null)
        pendingSimprintsMfidBiometricIdentification = null
        sessionRepository.clear()
    }

    fun clearPendingSessionIfNeeded(searchItems: List<FieldUiModel>) {
        val searchFields = searchItems.toSearchFields()
        val searchState = SimprintsSearchUtils.searchState(searchFields)
        if (sessionRepository.hasPendingSession() && searchState.shouldClearPendingSession) {
            pendingSimprintsMfidBiometricIdentification = null
            sessionRepository.clear()
        }
    }

    fun refreshSimprintsUiState(searchItems: List<FieldUiModel>) {
        clearPendingSessionIfNeeded(searchItems)
        val searchFields = searchItems.toSearchFields()
        val searchState = SimprintsSearchUtils.searchState(searchFields)
        _isSimprintsBiometricSearch.postValue(searchState.hasBiometricIdentificationQuery)
        _isSimprintsUseLastBiometricsLabel.postValue(
            SimprintsSearchUtils.shouldUseLastBiometricsLabel(
                searchState = searchState,
                hasPendingSession = sessionRepository.hasPendingSession(),
            ),
        )
    }

    fun onSimprintsBiometricIdentificationResult(
        uid: String,
        value: String?,
        hasAutoOpenEligibleSimprintsIdentification: Boolean,
    ) {
        pendingSimprintsMfidBiometricIdentification =
            if (!value.isNullOrBlank() && hasAutoOpenEligibleSimprintsIdentification) {
                PendingSimprintsMfidBiometricIdentification(uid = uid, value = value)
            } else {
                null
            }
    }

    suspend fun onSimprintsParameterSaved(
        uid: String,
        value: String?,
        searchItems: List<FieldUiModel>,
        initialProgramUid: String?,
        queryData: Map<String, List<String>?>,
    ): PendingDashboardNavigation? {
        val searchFields = searchItems.toSearchFields()
        if (!shouldAutoNavigateToSimprintsBiometricSearch(uid, value, searchFields)) {
            return null
        }

        if (consumePendingSimprintsMfidBiometricIdentification(uid, value)) {
            return resolveSingleBiometricSearchNavigation(
                initialProgramUid = initialProgramUid,
                queryData = queryData,
                value = value,
            )?.let { navigationTarget ->
                PendingDashboardNavigation(
                    teiUid = navigationTarget.teiUid,
                    programUid = navigationTarget.programUid,
                    enrollmentUid = navigationTarget.enrollmentUid,
                )
            }?.also {
                clearPendingSession()
            } ?: requestSimprintsBiometricSearchNavigation()
        }

        requestSimprintsBiometricSearchNavigation()
        return null
    }

    fun clearSimprintsBiometricQueryData(
        searchItems: List<FieldUiModel>,
        queryData: MutableMap<String, List<String>?>,
    ): List<FieldUiModel>? {
        val simprintsBiometricFieldUids =
            searchItems
                .filter { field ->
                    SimprintsIntentUtils.isIdentifyCallout(field.customIntent)
                }.map(FieldUiModel::uid)
                .toSet()

        if (simprintsBiometricFieldUids.isEmpty()) {
            return null
        }

        queryData.keys.removeAll(simprintsBiometricFieldUids)
        clearPendingSession()

        return searchItems.map { field ->
            if (field.uid in simprintsBiometricFieldUids) {
                (field as FieldUiModelImpl).copy(value = null, displayName = null)
            } else {
                field
            }
        }
    }

    fun shouldUseLastBiometricsLabel(searchItems: List<FieldUiModel>): Boolean {
        val searchState = SimprintsSearchUtils.searchState(searchItems.toSearchFields())
        return SimprintsSearchUtils.shouldUseLastBiometricsLabel(
            searchState = searchState,
            hasPendingSession = sessionRepository.hasPendingSession(),
        )
    }

    private fun shouldAutoNavigateToSimprintsBiometricSearch(
        uid: String,
        value: String?,
        searchFields: List<SimprintsSearchUtils.SearchField>,
    ): Boolean =
        !value.isNullOrBlank() &&
            searchFields
                .firstOrNull { it.uid == uid }
                ?.customIntent
                ?.let(SimprintsIntentUtils::isIdentifyCallout) == true

    private fun consumePendingSimprintsMfidBiometricIdentification(
        uid: String,
        value: String?,
    ): Boolean =
        pendingSimprintsMfidBiometricIdentification
            ?.takeIf { it.uid == uid && it.value == value }
            ?.let {
                pendingSimprintsMfidBiometricIdentification = null
                true
            } ?: false

    private suspend fun requestSimprintsBiometricSearchNavigation(): PendingDashboardNavigation? {
        _simprintsBiometricSearchNavigation.send(Unit)
        return null
    }

    private fun List<FieldUiModel>.toSearchFields(): List<SimprintsSearchUtils.SearchField> =
        map { field ->
            SimprintsSearchUtils.SearchField(
                uid = field.uid,
                value = field.value,
                customIntent = field.customIntent,
            )
        }
}
