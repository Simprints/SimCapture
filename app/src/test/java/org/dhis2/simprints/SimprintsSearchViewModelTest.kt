package org.dhis2.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import kotlinx.coroutines.test.runTest
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolveConfirmIdentityCalloutUseCase
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimprintsSearchViewModelTest {
    private val resolveConfirmIdentityCallout: SimprintsResolveConfirmIdentityCalloutUseCase =
        mock()
    private val sessionRepository: SimprintsSessionRepository = mock()

    @Test
    fun `onDashboardRequested should launch confirm identity once and clear pending session`() =
        runTest {
            val launchIntent: Intent = mock()
            whenever(sessionRepository.get()) doReturn "session-id"
            whenever(resolveConfirmIdentityCallout.invoke(any(), any(), any())) doReturn
                SimprintsIntentUtils.PreparedCallout(
                    launchIntent = launchIntent,
                    responseData = emptyList(),
                )
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                )

            val action =
                viewModel.onDashboardRequested(
                    searchFields = listOf(identifyField(value = "guid-1")),
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    enrollmentUid = "enrollment-uid",
                )

            assertTrue(action is SimprintsSearchViewModel.DashboardAction.LaunchConfirmIdentity)
            assertSame(
                launchIntent,
                (action as SimprintsSearchViewModel.DashboardAction.LaunchConfirmIdentity).intent,
            )
            verify(sessionRepository).clear()

            val navigation = viewModel.onConfirmIdentityResult(RESULT_OK)
            assertEquals("tei-uid", navigation?.teiUid)
            assertEquals("program-uid", navigation?.programUid)
            assertEquals("enrollment-uid", navigation?.enrollmentUid)
            assertNull(viewModel.onConfirmIdentityResult(RESULT_OK))
        }

    @Test
    fun `onDashboardRequested should open dashboard directly after confirm identity already cleared session`() =
        runTest {
            whenever(sessionRepository.get()).thenReturn("session-id").thenReturn(null)
            whenever(resolveConfirmIdentityCallout.invoke(any(), any(), any())) doReturn
                SimprintsIntentUtils.PreparedCallout(
                    launchIntent = mock(),
                    responseData = emptyList(),
                )
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                )

            val firstAction =
                viewModel.onDashboardRequested(
                    searchFields = listOf(identifyField(value = "guid-1")),
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    enrollmentUid = "enrollment-uid",
                )
            val secondAction =
                viewModel.onDashboardRequested(
                    searchFields = listOf(identifyField(value = "guid-1")),
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    enrollmentUid = "enrollment-uid",
                )

            assertTrue(firstAction is SimprintsSearchViewModel.DashboardAction.LaunchConfirmIdentity)
            assertTrue(secondAction is SimprintsSearchViewModel.DashboardAction.OpenDashboard)
            verify(sessionRepository).clear()
        }

    @Test
    fun `onDashboardRequested should open dashboard when confirm identity is not available`() =
        runTest {
            whenever(sessionRepository.get()) doReturn "session-id"
            whenever(resolveConfirmIdentityCallout.invoke(any(), any(), any())) doReturn null
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                )

            val action =
                viewModel.onDashboardRequested(
                    searchFields = listOf(identifyField(value = "guid-1")),
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    enrollmentUid = "enrollment-uid",
                )

            assertTrue(action is SimprintsSearchViewModel.DashboardAction.OpenDashboard)
            verify(sessionRepository, never()).clear()
        }

    @Test
    fun `prepareEnrollmentQueryData should mark pending enrollment and strip biometric fields`() {
        whenever(sessionRepository.hasPendingSession()) doReturn true
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
            )

        val filteredQueryData =
            viewModel.prepareEnrollmentQueryData(
                searchFields =
                    listOf(
                        identifyField(value = "guid-1"),
                        textField(uid = "name", value = "Alice"),
                    ),
                queryData =
                    mapOf(
                        "biometric" to listOf("guid-1"),
                        "name" to listOf("Alice"),
                    ),
            )

        assertEquals(hashMapOf("name" to listOf("Alice")), filteredQueryData)
        verify(sessionRepository).markPendingEnrollment()
    }

    @Test
    fun `prepareEnrollmentQueryData should keep non biometric searches unchanged`() {
        whenever(sessionRepository.hasPendingSession()) doReturn true
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
            )

        val filteredQueryData =
            viewModel.prepareEnrollmentQueryData(
                searchFields = listOf(textField(uid = "name", value = "Alice")),
                queryData = mapOf("name" to listOf("Alice")),
            )

        assertEquals(hashMapOf("name" to listOf("Alice")), filteredQueryData)
        verify(sessionRepository, never()).markPendingEnrollment()
    }

    @Test
    fun `clearPendingSessionIfNeeded should clear pending session when query is no longer biometric`() {
        whenever(sessionRepository.hasPendingSession()) doReturn true
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
            )

        viewModel.clearPendingSessionIfNeeded(
            searchFields = listOf(textField(uid = "name", value = "Name")),
        )

        verify(sessionRepository).clear()
    }

    @Test
    fun `shouldUseLastBiometricsLabel should return false for non biometric search`() {
        whenever(sessionRepository.hasPendingSession()) doReturn true
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
            )

        val shouldUseLastBiometricsLabel =
            viewModel.shouldUseLastBiometricsLabel(
                searchFields = listOf(textField(uid = "name", value = "Alice")),
            )

        assertFalse(shouldUseLastBiometricsLabel)
        verify(sessionRepository, never()).clear()
    }

    @Test
    fun `shouldUseLastBiometricsLabel should return true only for pending biometric search`() {
        whenever(sessionRepository.hasPendingSession()) doReturn true
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
            )

        val shouldUseLastBiometricsLabel =
            viewModel.shouldUseLastBiometricsLabel(
                searchFields = listOf(identifyField(value = "guid-1")),
            )

        assertTrue(shouldUseLastBiometricsLabel)
    }

    @Test
    fun `onConfirmIdentityResult should clear pending navigation when cancelled`() =
        runTest {
            whenever(sessionRepository.get()) doReturn "session-id"
            whenever(resolveConfirmIdentityCallout.invoke(any(), any(), any())) doReturn
                SimprintsIntentUtils.PreparedCallout(
                    launchIntent = mock(),
                    responseData = emptyList(),
                )
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                )

            viewModel.onDashboardRequested(
                searchFields = listOf(identifyField(value = "guid-1")),
                teiUid = "tei-uid",
                programUid = "program-uid",
                enrollmentUid = "enrollment-uid",
            )

            assertNull(viewModel.onConfirmIdentityResult(resultCode = 0))
            assertNull(viewModel.onConfirmIdentityResult(RESULT_OK))
        }

    private fun identifyField(value: String?) =
        SimprintsSearchUtils.SearchField(
            uid = "biometric",
            value = value,
            customIntent = identifyIntent(),
        )

    private fun textField(
        uid: String,
        value: String?,
    ) = SimprintsSearchUtils.SearchField(
        uid = uid,
        value = value,
        customIntent = null,
    )

    private fun identifyIntent() =
        CustomIntentModel(
            uid = "identify-intent",
            name = "Identify",
            packageName = "com.simprints.id.IDENTIFY",
            customIntentRequest = emptyList(),
            customIntentResponse =
                listOf(
                    CustomIntentResponseDataModel(
                        name = "guid",
                        extraType = CustomIntentResponseExtraType.STRING,
                        key = null,
                    ),
                ),
        )
}
