package org.dhis2.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import kotlinx.coroutines.test.runTest
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolvePendingEnrollmentActionUseCase
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimprintsEnrollmentViewModelTest {
    private val simprintsD2Repository: SimprintsD2Repository = mock()
    private val resolvePendingEnrollmentAction: SimprintsResolvePendingEnrollmentActionUseCase =
        mock()
    private val sessionRepository: SimprintsSessionRepository = mock()
    private val resultMapper: SimprintsCustomIntentResultMapper = mock()

    @Test
    fun `onFinishRequested should return null when there is no pending enrollment session`() =
        runTest {
            whenever(sessionRepository.pendingEnrollmentSessionId()) doReturn null
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            val launchIntent =
                viewModel.onFinishRequested(
                    enrollmentUid = "enrollment-uid",
                )

            assertNull(launchIntent)
            verify(resolvePendingEnrollmentAction, never()).invoke(any(), any())
        }

    @Test
    fun `onAutoEnrollLastRequested should return null when there is no session`() =
        runTest {
            whenever(sessionRepository.get()) doReturn null
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            val launchIntent =
                viewModel.onAutoEnrollLastRequested(
                    enrollmentUid = "enrollment-uid",
                )

            assertNull(launchIntent)
            verify(resolvePendingEnrollmentAction, never()).invoke(any(), any())
        }

    @Test
    fun `onRegisterLastResult should save mapped value clear session and continue finish`() =
        runTest {
            val launchIntent: Intent = mock()
            val resultIntent: Intent = mock()
            val responseData =
                listOf(
                    CustomIntentResponseDataModel(
                        name = "subjectId",
                        extraType = CustomIntentResponseExtraType.STRING,
                        key = null,
                    ),
                )
            val pendingAction =
                SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction(
                    fieldUid = "attribute-uid",
                    callout =
                        SimprintsIntentUtils.PreparedCallout(
                            launchIntent = launchIntent,
                            responseData = responseData,
                        ),
                )
            whenever(sessionRepository.pendingEnrollmentSessionId()) doReturn "session-id"
            whenever(
                resolvePendingEnrollmentAction.invoke(
                    "enrollment-uid",
                    "session-id",
                ),
            ) doReturn pendingAction
            whenever(resultMapper.map(responseData, resultIntent)) doReturn "subject-guid"
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            val preparedIntent =
                viewModel.onFinishRequested(
                    enrollmentUid = "enrollment-uid",
                )
            val result =
                viewModel.onRegisterLastResult(
                    resultCode = RESULT_OK,
                    data = resultIntent,
                    teiUid = "tei-uid",
                )

            assertSame(launchIntent, preparedIntent)
            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.CONTINUE_FINISH, result)
            verify(simprintsD2Repository).saveTrackedEntityAttributeValue(
                teiUid = "tei-uid",
                attributeUid = "attribute-uid",
                value = "subject-guid",
            )
            verify(sessionRepository).clear()
        }

    @Test
    fun `onAutoEnrollLastRequested should save mapped value clear session and continue finish`() =
        runTest {
            val launchIntent: Intent = mock()
            val resultIntent: Intent = mock()
            val responseData =
                listOf(
                    CustomIntentResponseDataModel(
                        name = "subjectId",
                        extraType = CustomIntentResponseExtraType.STRING,
                        key = null,
                    ),
                )
            val pendingAction =
                SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction(
                    fieldUid = "attribute-uid",
                    callout =
                        SimprintsIntentUtils.PreparedCallout(
                            launchIntent = launchIntent,
                            responseData = responseData,
                        ),
                )
            whenever(sessionRepository.get()) doReturn "session-id"
            whenever(
                resolvePendingEnrollmentAction.invoke(
                    "enrollment-uid",
                    "session-id",
                ),
            ) doReturn pendingAction
            whenever(resultMapper.map(responseData, resultIntent)) doReturn "subject-guid"
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            val preparedIntent =
                viewModel.onAutoEnrollLastRequested(
                    enrollmentUid = "enrollment-uid",
                )
            val result =
                viewModel.onRegisterLastResult(
                    resultCode = RESULT_OK,
                    data = resultIntent,
                    teiUid = "tei-uid",
                )

            assertSame(launchIntent, preparedIntent)
            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.CONTINUE_FINISH, result)
            verify(simprintsD2Repository).saveTrackedEntityAttributeValue(
                teiUid = "tei-uid",
                attributeUid = "attribute-uid",
                value = "subject-guid",
            )
            verify(sessionRepository).clear()
        }

    @Test
    fun `onRegisterLastResult should return error when mapped value is missing`() =
        runTest {
            val resultIntent: Intent = mock()
            val responseData =
                listOf(
                    CustomIntentResponseDataModel(
                        name = "subjectId",
                        extraType = CustomIntentResponseExtraType.STRING,
                        key = null,
                    ),
                )
            val pendingAction =
                SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction(
                    fieldUid = "attribute-uid",
                    callout =
                        SimprintsIntentUtils.PreparedCallout(
                            launchIntent = mock(),
                            responseData = responseData,
                        ),
                )
            whenever(sessionRepository.pendingEnrollmentSessionId()) doReturn "session-id"
            whenever(
                resolvePendingEnrollmentAction.invoke(
                    "enrollment-uid",
                    "session-id",
                ),
            ) doReturn pendingAction
            whenever(resultMapper.map(responseData, resultIntent)) doReturn null
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            viewModel.onFinishRequested(
                enrollmentUid = "enrollment-uid",
            )
            val result =
                viewModel.onRegisterLastResult(
                    resultCode = RESULT_OK,
                    data = resultIntent,
                    teiUid = "tei-uid",
                )

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.ERROR, result)
            verify(simprintsD2Repository, never()).saveTrackedEntityAttributeValue(
                any(),
                any(),
                any(),
            )
            verify(sessionRepository, never()).clear()
        }

    @Test
    fun `onRegisterLastResult should return error when tei uid is missing`() =
        runTest {
            val pendingAction =
                SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction(
                    fieldUid = "attribute-uid",
                    callout =
                        SimprintsIntentUtils.PreparedCallout(
                            launchIntent = mock(),
                            responseData = emptyList(),
                        ),
                )
            whenever(sessionRepository.pendingEnrollmentSessionId()) doReturn "session-id"
            whenever(
                resolvePendingEnrollmentAction.invoke(
                    "enrollment-uid",
                    "session-id",
                ),
            ) doReturn pendingAction
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            viewModel.onFinishRequested(
                enrollmentUid = "enrollment-uid",
            )
            val result =
                viewModel.onRegisterLastResult(
                    resultCode = RESULT_OK,
                    data = mock(),
                    teiUid = null,
                )

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.ERROR, result)
            verify(resultMapper, never()).map(any(), any())
        }

    @Test
    fun `onRegisterLastResult should return none when there is no pending action`() =
        runTest {
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            val result =
                viewModel.onRegisterLastResult(
                    resultCode = RESULT_OK,
                    data = mock(),
                    teiUid = "tei-uid",
                )

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.NONE, result)
        }

    @Test
    fun `onRegisterLastLaunchFailed should discard pending action`() =
        runTest {
            val pendingAction =
                SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction(
                    fieldUid = "attribute-uid",
                    callout =
                        SimprintsIntentUtils.PreparedCallout(
                            launchIntent = mock(),
                            responseData = emptyList(),
                        ),
                )
            whenever(sessionRepository.pendingEnrollmentSessionId()) doReturn "session-id"
            whenever(
                resolvePendingEnrollmentAction.invoke(
                    "enrollment-uid",
                    "session-id",
                ),
            ) doReturn pendingAction
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            viewModel.onFinishRequested(
                enrollmentUid = "enrollment-uid",
            )
            viewModel.onRegisterLastLaunchFailed()
            val result =
                viewModel.onRegisterLastResult(
                    resultCode = RESULT_OK,
                    data = mock(),
                    teiUid = "tei-uid",
                )

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.NONE, result)
        }
}
