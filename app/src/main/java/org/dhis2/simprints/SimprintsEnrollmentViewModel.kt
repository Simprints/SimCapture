package org.dhis2.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.lifecycle.ViewModel
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolvePendingEnrollmentActionUseCase
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class SimprintsEnrollmentViewModel(
    private val simprintsD2Repository: SimprintsD2Repository,
    private val resolvePendingEnrollmentAction: SimprintsResolvePendingEnrollmentActionUseCase,
    private val sessionRepository: SimprintsSessionRepository,
    private val resultMapper: SimprintsCustomIntentResultMapper,
) : ViewModel() {
    enum class RegisterLastResult {
        NONE,
        CONTINUE_FINISH,
        ERROR,
    }

    private val pendingAction =
        AtomicReference<SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction?>(null)

    suspend fun onFinishRequested(enrollmentUid: String): Intent? {
        val resolvedAction =
            sessionRepository.pendingEnrollmentSessionId()?.let {
                resolvePendingEnrollmentAction(
                    enrollmentUid = enrollmentUid,
                    sessionId = it,
                )
            } ?: return null

        pendingAction.store(resolvedAction)
        return resolvedAction.callout.launchIntent
    }

    suspend fun onAutoEnrollLastRequested(enrollmentUid: String): Intent? {
        val sessionId = sessionRepository.get()?.takeIf(String::isNotBlank) ?: return null
        val resolvedAction =
            resolvePendingEnrollmentAction(
                enrollmentUid = enrollmentUid,
                sessionId = sessionId,
            ) ?: return null

        pendingAction.store(resolvedAction)
        sessionRepository.markPendingEnrollmentFromPossibleDuplicates()
        return resolvedAction.callout.launchIntent
    }

    suspend fun onRegisterLastResult(
        resultCode: Int,
        data: Intent?,
        teiUid: String?,
        enrollmentUid: String?,
    ): RegisterLastResult {
        val resolvedAction =
            pendingAction.exchange(null)
                ?: restorePendingAction(enrollmentUid)
                ?: return RegisterLastResult.NONE
        if (resultCode != RESULT_OK) {
            sessionRepository.clearPendingEnrollment()
            return RegisterLastResult.ERROR
        }

        return try {
            val resolvedTeiUid =
                teiUid ?: enrollmentUid?.let { simprintsD2Repository.getEnrollmentContext(it)?.teiUid }
            if (resolvedTeiUid == null) {
                sessionRepository.clearPendingEnrollment()
                return RegisterLastResult.ERROR
            }
            val value =
                resultMapper.map(
                    responseData = resolvedAction.callout.responseData,
                    data = data,
                )
            if (value == null) {
                sessionRepository.clearPendingEnrollment()
                RegisterLastResult.ERROR
            } else {
                simprintsD2Repository.saveTrackedEntityAttributeValue(
                    teiUid = resolvedTeiUid,
                    attributeUid = resolvedAction.fieldUid,
                    value = value,
                )
                sessionRepository.clear()
                RegisterLastResult.CONTINUE_FINISH
            }
        } catch (e: Exception) {
            sessionRepository.clearPendingEnrollment()
            throw e
        }
    }

    fun onRegisterLastLaunchFailed() {
        pendingAction.store(null)
        sessionRepository.clearPendingEnrollment()
    }

    private suspend fun restorePendingAction(
        enrollmentUid: String?,
    ): SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction? {
        val resolvedEnrollmentUid = enrollmentUid?.takeIf(String::isNotBlank) ?: return null
        val sessionId = sessionRepository.pendingEnrollmentSessionId()?.takeIf(String::isNotBlank) ?: return null
        return resolvePendingEnrollmentAction(
            enrollmentUid = resolvedEnrollmentUid,
            sessionId = sessionId,
        )
    }
}
