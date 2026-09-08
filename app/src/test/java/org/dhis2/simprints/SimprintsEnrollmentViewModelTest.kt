package org.dhis2.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
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
import org.mockito.kotlin.inOrder
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
                    enrollmentUid = "enrollment-uid",
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
                    enrollmentUid = "enrollment-uid",
                )

            assertSame(launchIntent, preparedIntent)
            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.CONTINUE_FINISH, result)
            verify(sessionRepository).markPendingEnrollmentFromPossibleDuplicates()
            verify(simprintsD2Repository).saveTrackedEntityAttributeValue(
                teiUid = "tei-uid",
                attributeUid = "attribute-uid",
                value = "subject-guid",
            )
            verify(sessionRepository).clear()
        }

    @Test
    fun `onRegisterLastResult should recover auto enroll last after recreation and save mapped value`() =
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
            whenever(resultMapper.map(responseData, resultIntent)) doReturn "subject-guid"
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
                    data = resultIntent,
                    teiUid = "tei-uid",
                    enrollmentUid = "enrollment-uid",
                )

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
                    enrollmentUid = "enrollment-uid",
                )

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.ERROR, result)
            verify(simprintsD2Repository, never()).saveTrackedEntityAttributeValue(
                any(),
                any(),
                any(),
            )
            verify(sessionRepository).clearPendingEnrollment()
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
                    enrollmentUid = "enrollment-uid",
                )

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.ERROR, result)
            verify(resultMapper, never()).map(any(), any())
            verify(sessionRepository).clearPendingEnrollment()
            verify(sessionRepository, never()).clear()
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
                    enrollmentUid = null,
                )

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.NONE, result)
        }

    @Test
    fun `onRegisterLastResult should recover lost pending action after recreation and clear pending enrollment on error`() =
        runTest {
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
            val viewModel =
                SimprintsEnrollmentViewModel(
                    simprintsD2Repository = simprintsD2Repository,
                    resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
                    sessionRepository = sessionRepository,
                    resultMapper = resultMapper,
                )

            val result =
                viewModel.onRegisterLastResult(
                    resultCode = 0,
                    data = mock(),
                    teiUid = "tei-uid",
                    enrollmentUid = "enrollment-uid",
                )

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.ERROR, result)
            verify(sessionRepository).clearPendingEnrollment()
            verify(simprintsD2Repository, never()).saveTrackedEntityAttributeValue(
                any(),
                any(),
                any(),
            )
        }

    @Test
    fun `onRegisterLastLaunchFailed should clear pending enrollment`() =
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

            verify(sessionRepository).clearPendingEnrollment()
        }

    @Test
    fun `pending finish should save GUID then SID external credential then clear session`() =
        runTest {
            val resultIntent = externalCredentialResult()
            val viewModel = prepareExternalCredentialResult(resultIntent)

            viewModel.onFinishRequested("enrollment-uid")

            assertExternalCredentialSavedInOrder(viewModel, resultIntent)
        }

    @Test
    fun `automatic Enrol Last should save GUID then SID external credential then clear session`() =
        runTest {
            val resultIntent = externalCredentialResult()
            val viewModel = prepareExternalCredentialResult(resultIntent)

            viewModel.onAutoEnrollLastRequested("enrollment-uid")

            assertExternalCredentialSavedInOrder(viewModel, resultIntent)
            verify(sessionRepository).markPendingEnrollmentFromPossibleDuplicates()
        }

    @Test
    fun `restored Enrol Last should save GUID then SID external credential then clear session`() =
        runTest {
            val resultIntent = externalCredentialResult()
            val viewModel = prepareExternalCredentialResult(resultIntent)

            assertExternalCredentialSavedInOrder(viewModel, resultIntent)
        }

    @Test
    fun `Enrol Last without external credential should save GUID without writing external credential`() =
        runTest {
            val resultIntent = externalCredentialResult(credentialJson = null)
            val viewModel = prepareExternalCredentialResult(resultIntent)

            val result = viewModel.onRegisterLastResult(RESULT_OK, resultIntent, "tei-uid", "enrollment-uid")

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.CONTINUE_FINISH, result)
            verify(simprintsD2Repository).saveTrackedEntityAttributeValue("tei-uid", "attribute-uid", "subject-guid")
            verify(simprintsD2Repository, never()).saveEnrollmentExternalCredential(any(), any(), any())
            verify(sessionRepository).clear()
        }

    @Test
    fun `unsuccessful Enrol Last must not save returned external credential`() =
        runTest {
            val resultIntent = externalCredentialResult()
            val viewModel = prepareExternalCredentialResult(resultIntent)

            val result = viewModel.onRegisterLastResult(0, resultIntent, "tei-uid", "enrollment-uid")

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.ERROR, result)
            verify(simprintsD2Repository, never()).saveTrackedEntityAttributeValue(any(), any(), any())
            verify(simprintsD2Repository, never()).saveEnrollmentExternalCredential(any(), any(), any())
            verify(sessionRepository).clearPendingEnrollment()
            verify(sessionRepository, never()).clear()
        }

    @Test
    fun `Enrol Last missing GUID must not save returned external credential`() =
        runTest {
            val resultIntent = externalCredentialResult()
            val viewModel = prepareExternalCredentialResult(resultIntent, guid = null)

            val result = viewModel.onRegisterLastResult(RESULT_OK, resultIntent, "tei-uid", "enrollment-uid")

            assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.ERROR, result)
            verify(simprintsD2Repository, never()).saveTrackedEntityAttributeValue(any(), any(), any())
            verify(simprintsD2Repository, never()).saveEnrollmentExternalCredential(any(), any(), any())
            verify(sessionRepository, never()).clear()
        }

    @Test
    fun `Enrol Last blank GUID must not save returned external credential`() =
        runTest {
            val resultIntent = externalCredentialResult()
            val viewModel = prepareExternalCredentialResult(resultIntent, guid = " ")

            viewModel.onRegisterLastResult(RESULT_OK, resultIntent, "tei-uid", "enrollment-uid")

            verify(simprintsD2Repository, never()).saveEnrollmentExternalCredential(any(), any(), any())
        }

    @Test
    fun `failed GUID save must not save external credential or clear successful session`() =
        runTest {
            val resultIntent = externalCredentialResult()
            val viewModel = prepareExternalCredentialResult(resultIntent)
            val error = IllegalStateException("GUID save failed")
            whenever(
                simprintsD2Repository.saveTrackedEntityAttributeValue("tei-uid", "attribute-uid", "subject-guid"),
            ).thenThrow(error)

            val result =
                runCatching {
                    viewModel.onRegisterLastResult(RESULT_OK, resultIntent, "tei-uid", "enrollment-uid")
                }

            assertSame(error, result.exceptionOrNull())
            verify(simprintsD2Repository, never()).saveEnrollmentExternalCredential(any(), any(), any())
            verify(sessionRepository).clearPendingEnrollment()
            verify(sessionRepository, never()).clear()
        }

    private suspend fun prepareExternalCredentialResult(
        resultIntent: Intent,
        guid: String? = "subject-guid",
    ): SimprintsEnrollmentViewModel {
        val launchIntent =
            mock<Intent> {
                on { action } doReturn "com.simprints.id.REGISTER_LAST_BIOMETRICS"
            }
        val pendingAction =
            SimprintsResolvePendingEnrollmentActionUseCase.PendingEnrollmentAction(
                fieldUid = "attribute-uid",
                callout =
                    SimprintsIntentUtils.PreparedCallout(
                        launchIntent = launchIntent,
                        responseData = emptyList(),
                    ),
            )
        whenever(sessionRepository.pendingEnrollmentSessionId()) doReturn "session-id"
        whenever(sessionRepository.get()) doReturn "session-id"
        whenever(resolvePendingEnrollmentAction("enrollment-uid", "session-id")) doReturn pendingAction
        whenever(resultMapper.map(emptyList(), resultIntent)) doReturn guid
        return SimprintsEnrollmentViewModel(
            simprintsD2Repository = simprintsD2Repository,
            resolvePendingEnrollmentAction = resolvePendingEnrollmentAction,
            sessionRepository = sessionRepository,
            resultMapper = resultMapper,
        )
    }

    private fun externalCredentialResult(
        credentialJson: String? = """{"type":"QRCode","value":"external-credential-1"}""",
        identificationKey: String? = null,
    ): Intent {
        val resultExtras =
            mock<Bundle> {
                on { getString("scannedCredential") } doReturn credentialJson
                on { containsKey("identification") } doReturn (identificationKey == "identification")
                on { containsKey("identifications") } doReturn (identificationKey == "identifications")
            }
        return mock {
            on { extras } doReturn resultExtras
        }
    }

    private suspend fun assertExternalCredentialSavedInOrder(
        viewModel: SimprintsEnrollmentViewModel,
        resultIntent: Intent,
    ) {
        val result = viewModel.onRegisterLastResult(RESULT_OK, resultIntent, "tei-uid", "enrollment-uid")

        assertEquals(SimprintsEnrollmentViewModel.RegisterLastResult.CONTINUE_FINISH, result)
        inOrder(simprintsD2Repository, sessionRepository).apply {
            verify(simprintsD2Repository).saveTrackedEntityAttributeValue("tei-uid", "attribute-uid", "subject-guid")
            verify(simprintsD2Repository).saveEnrollmentExternalCredential("enrollment-uid", "attribute-uid", "external-credential-1")
            verify(sessionRepository).clear()
        }
    }
}
