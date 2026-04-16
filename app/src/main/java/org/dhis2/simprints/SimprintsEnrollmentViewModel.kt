package org.dhis2.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolvePendingEnrollmentActionUseCase

class SimprintsEnrollmentViewModel(
    private val simprintsD2Repository: SimprintsD2Repository,
    private val resolvePendingEnrollmentAction: SimprintsResolvePendingEnrollmentActionUseCase,
    private val sessionRepository: SimprintsSessionRepository,
    private val resultMapper: SimprintsCustomIntentResultMapper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    enum class RegisterLastResult {
        NONE,
        CONTINUE_FINISH,
        ERROR,
    }

    private var pendingAction: SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction? = null

    suspend fun onFinishRequested(
        isNewEnrollment: Boolean,
        enrollmentUid: String,
    ): Intent? {
        if (!isNewEnrollment) {
            return null
        }

        val resolvedAction =
            sessionRepository.pendingEnrollmentSessionId()?.let {
                resolvePendingEnrollmentAction(
                    enrollmentUid = enrollmentUid,
                    sessionId = it,
                )
            } ?: return null

        pendingAction = resolvedAction
        return resolvedAction.callout.launchIntent
    }

    suspend fun onRegisterLastResult(
        resultCode: Int,
        data: Intent?,
        teiUid: String?,
    ): RegisterLastResult {
        val resolvedAction = pendingAction ?: return RegisterLastResult.NONE
        pendingAction = null

        val saved =
            if (resultCode == RESULT_OK && teiUid != null) {
                withContext(ioDispatcher) {
                    val value =
                        resultMapper.map(
                            responseData = resolvedAction.callout.responseData,
                            data = data,
                        ) ?: return@withContext false
                    simprintsD2Repository.saveTrackedEntityAttributeValue(
                        teiUid = teiUid,
                        attributeUid = resolvedAction.fieldUid,
                        value = value,
                    )
                    true
                }
            } else {
                false
            }

        if (saved) {
            sessionRepository.clear()
            return RegisterLastResult.CONTINUE_FINISH
        }

        return RegisterLastResult.ERROR
    }

    fun onRegisterLastLaunchFailed() {
        pendingAction = null
    }
}
