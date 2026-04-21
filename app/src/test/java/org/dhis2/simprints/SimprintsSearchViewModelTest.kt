package org.dhis2.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolveConfirmIdentityCalloutUseCase
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.form.model.FieldUiModelImpl
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.hisp.dhis.android.core.common.ValueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimprintsSearchViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val resolveConfirmIdentityCallout: SimprintsResolveConfirmIdentityCalloutUseCase =
        mock()
    private val sessionRepository: SimprintsSessionRepository = mock()
    private val resolveSingleBiometricSearchNavigation: SimprintsResolveSingleBiometricSearchNavigationUseCase =
        mock()

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
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )

            val action =
                viewModel.onDashboardRequested(
                    searchItems = listOf(identifyField(value = "guid-1")),
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
    fun `onDashboardRequested should keep session when requested`() =
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
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )

            val action =
                viewModel.onDashboardRequested(
                    searchItems = listOf(identifyField(value = "guid-1")),
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    enrollmentUid = "enrollment-uid",
                    keepSession = true,
                )

            assertTrue(action is SimprintsSearchViewModel.DashboardAction.LaunchConfirmIdentity)
            verify(sessionRepository, never()).clear()
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
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )

            val firstAction =
                viewModel.onDashboardRequested(
                    searchItems = listOf(identifyField(value = "guid-1")),
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    enrollmentUid = "enrollment-uid",
                )
            val secondAction =
                viewModel.onDashboardRequested(
                    searchItems = listOf(identifyField(value = "guid-1")),
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
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )

            val action =
                viewModel.onDashboardRequested(
                    searchItems = listOf(identifyField(value = "guid-1")),
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
                resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            )

        val filteredQueryData =
            viewModel.prepareEnrollmentQueryData(
                searchItems =
                    listOf(
                        identifyField(value = "guid-1"),
                        textField(uid = "name", value = "Name"),
                    ),
                queryData =
                    mapOf(
                        "biometric" to listOf("guid-1"),
                        "name" to listOf("Name"),
                    ),
            )

        assertEquals(hashMapOf("name" to listOf("Name")), filteredQueryData)
        verify(sessionRepository).markPendingEnrollment()
    }

    @Test
    fun `prepareEnrollmentQueryData should keep non biometric searches unchanged`() {
        whenever(sessionRepository.hasPendingSession()) doReturn true
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
                resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            )

        val filteredQueryData =
            viewModel.prepareEnrollmentQueryData(
                searchItems = listOf(textField(uid = "name", value = "Name")),
                queryData = mapOf("name" to listOf("Name")),
            )

        assertEquals(hashMapOf("name" to listOf("Name")), filteredQueryData)
        verify(sessionRepository, never()).markPendingEnrollment()
    }

    @Test
    fun `clearPendingSessionIfNeeded should clear pending session when query is no longer biometric`() {
        whenever(sessionRepository.hasPendingSession()) doReturn true
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
                resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            )

        viewModel.clearPendingSessionIfNeeded(
            searchItems = listOf(textField(uid = "name", value = "Name")),
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
                resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            )

        val shouldUseLastBiometricsLabel =
            viewModel.shouldUseLastBiometricsLabel(
                searchItems = listOf(textField(uid = "name", value = "Name")),
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
                resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            )

        val shouldUseLastBiometricsLabel =
            viewModel.shouldUseLastBiometricsLabel(
                searchItems = listOf(identifyField(value = "guid-1")),
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
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )

            viewModel.onDashboardRequested(
                searchItems = listOf(identifyField(value = "guid-1")),
                teiUid = "tei-uid",
                programUid = "program-uid",
                enrollmentUid = "enrollment-uid",
            )

            assertNull(viewModel.onConfirmIdentityResult(resultCode = 0))
            assertNull(viewModel.onConfirmIdentityResult(RESULT_OK))
        }

    @Test
    fun `onConfirmIdentityLaunchFailed should clear pending dashboard navigation`() =
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
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )

            viewModel.onDashboardRequested(
                searchItems = listOf(identifyField(value = "guid-1")),
                teiUid = "tei-uid",
                programUid = "program-uid",
                enrollmentUid = "enrollment-uid",
            )
            viewModel.onConfirmIdentityLaunchFailed()

            assertNull(viewModel.onConfirmIdentityResult(RESULT_OK))
        }

    @Test
    fun `refreshSimprintsUiState should expose Simprints biometric search state`() {
        whenever(sessionRepository.hasPendingSession()) doReturn true
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
                resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            )

        viewModel.refreshSimprintsUiState(
            searchItems = listOf(identifyField(value = "guid-1")),
        )

        assertTrue(viewModel.isSimprintsBiometricSearch.value == true)
        assertTrue(viewModel.isSimprintsUseLastBiometricsLabel.value == true)
    }

    @Test
    fun `onSimprintsParameterSaved should emit Simprints biometric search navigation when biometric values are saved`() =
        runTest {
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )

            viewModel.simprintsBiometricSearchNavigation.test {
                val navigation =
                    viewModel.onSimprintsParameterSaved(
                        uid = "biometric",
                        value = "guid-1,guid-2",
                        searchItems = listOf(identifyField(value = "guid-1,guid-2")),
                        initialProgramUid = "program-uid",
                        queryData = mapOf("biometric" to listOf("guid-1", "guid-2")),
                    )

                assertNull(navigation)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSimprintsParameterSaved should ignore non Simprints fields`() =
        runTest {
            whenever(
                resolveSingleBiometricSearchNavigation(
                    initialProgramUid = any(),
                    queryData = any(),
                    value = any(),
                ),
            ) doReturn
                SimprintsResolveSingleBiometricSearchNavigationUseCase.NavigationTarget(
                    teiUid = "teiUid",
                    programUid = "programUid",
                    enrollmentUid = "enrollmentUid",
                    isOnline = false,
                )
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )

            viewModel.simprintsBiometricSearchNavigation.test {
                val navigation =
                    viewModel.onSimprintsParameterSaved(
                        uid = "name",
                        value = "Name",
                        searchItems = listOf(textField(uid = "name", value = "Name")),
                        initialProgramUid = "program-uid",
                        queryData = mapOf("name" to listOf("Name")),
                    )

                assertNull(navigation)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSimprintsBiometricIdentificationResult should clear pending direct navigation when value becomes blank`() =
        runTest {
            whenever(
                resolveSingleBiometricSearchNavigation(
                    initialProgramUid = any(),
                    queryData = any(),
                    value = any(),
                ),
            ) doReturn
                SimprintsResolveSingleBiometricSearchNavigationUseCase.NavigationTarget(
                    teiUid = "teiUid",
                    programUid = "programUid",
                    enrollmentUid = "enrollmentUid",
                    isOnline = false,
                )
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )
            viewModel.onSimprintsBiometricIdentificationResult(
                uid = "biometric",
                value = "guid-1",
                hasAutoOpenEligibleSimprintsIdentification = true,
            )
            viewModel.onSimprintsBiometricIdentificationResult(
                uid = "biometric",
                value = "",
                hasAutoOpenEligibleSimprintsIdentification = true,
            )

            viewModel.simprintsBiometricSearchNavigation.test {
                val navigation =
                    viewModel.onSimprintsParameterSaved(
                        uid = "biometric",
                        value = "guid-1",
                        searchItems = listOf(identifyField(value = "guid-1")),
                        initialProgramUid = "program-uid",
                        queryData = mapOf("biometric" to listOf("guid-1")),
                    )

                assertNull(navigation)
                awaitItem()
                verify(resolveSingleBiometricSearchNavigation, never()).invoke(any(), any(), any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSimprintsParameterSaved should open single Simprints MFID biometric search result directly when eligible`() =
        runTest {
            whenever(
                resolveSingleBiometricSearchNavigation(
                    initialProgramUid = any(),
                    queryData = any(),
                    value = any(),
                ),
            ) doReturn
                SimprintsResolveSingleBiometricSearchNavigationUseCase.NavigationTarget(
                    teiUid = "teiUid",
                    programUid = "programUid",
                    enrollmentUid = "enrollmentUid",
                    isOnline = false,
                )
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )
            viewModel.onSimprintsBiometricIdentificationResult(
                uid = "biometric",
                value = "guid-1",
                hasAutoOpenEligibleSimprintsIdentification = true,
            )

            val navigation =
                viewModel.onSimprintsParameterSaved(
                    uid = "biometric",
                    value = "guid-1",
                    searchItems = listOf(identifyField(value = "guid-1")),
                    initialProgramUid = "program-uid",
                    queryData = mapOf("biometric" to listOf("guid-1")),
                )

            assertEquals("teiUid", navigation?.teiUid)
            assertEquals("programUid", navigation?.programUid)
            assertEquals("enrollmentUid", navigation?.enrollmentUid)
            verify(sessionRepository).clear()
        }

    @Test
    fun `onSimprintsParameterSaved should open pending MFID biometric result directly even before search items are restored`() =
        runTest {
            whenever(
                resolveSingleBiometricSearchNavigation(
                    initialProgramUid = any(),
                    queryData = any(),
                    value = any(),
                ),
            ) doReturn
                SimprintsResolveSingleBiometricSearchNavigationUseCase.NavigationTarget(
                    teiUid = "teiUid",
                    programUid = "programUid",
                    enrollmentUid = "enrollmentUid",
                    isOnline = true,
                )
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )
            viewModel.onSimprintsBiometricIdentificationResult(
                uid = "biometric",
                value = "guid-1",
                hasAutoOpenEligibleSimprintsIdentification = true,
            )

            val navigation =
                viewModel.onSimprintsParameterSaved(
                    uid = "biometric",
                    value = "guid-1",
                    searchItems = emptyList(),
                    initialProgramUid = "program-uid",
                    queryData = mapOf("biometric" to listOf("guid-1")),
                )

            assertEquals("teiUid", navigation?.teiUid)
            assertEquals("programUid", navigation?.programUid)
            assertEquals("enrollmentUid", navigation?.enrollmentUid)
            assertTrue(navigation?.isOnline == true)
            verify(sessionRepository).clear()
        }

    @Test
    fun `clearSimprintsBiometricQueryData should clear only biometric search items`() {
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
                resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            )
        val queryData =
            mutableMapOf<String, List<String>?>(
                "biometric" to listOf("guid-1"),
                "name" to listOf("Name"),
            )

        val updatedItems =
            viewModel.clearSimprintsBiometricQueryData(
                searchItems =
                    listOf(
                        identifyField(value = "guid-1"),
                        textField(uid = "name", value = "Name"),
                    ),
                queryData = queryData,
            )

        assertEquals(mapOf("name" to listOf("Name")), queryData)
        assertEquals(null, updatedItems?.first()?.value)
        assertEquals("Name", updatedItems?.get(1)?.value)
        verify(sessionRepository).clear()
    }

    @Test
    fun `clearSimprintsBiometricQueryData should do nothing when search has no Simprints identify field`() {
        val viewModel =
            SimprintsSearchViewModel(
                resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                sessionRepository = sessionRepository,
                resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
            )
        val queryData = mutableMapOf<String, List<String>?>("name" to listOf("Name"))

        val updatedItems =
            viewModel.clearSimprintsBiometricQueryData(
                searchItems = listOf(textField(uid = "name", value = "Name")),
                queryData = queryData,
            )

        assertNull(updatedItems)
        assertEquals(mapOf("name" to listOf("Name")), queryData)
        verify(sessionRepository, never()).clear()
    }

    @Test
    fun `onSimprintsParameterSaved should request Simprints biometric search when eligible MFID resolution does not open directly`() =
        runTest {
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )
            viewModel.onSimprintsBiometricIdentificationResult(
                uid = "biometric",
                value = "guid-1",
                hasAutoOpenEligibleSimprintsIdentification = true,
            )

            viewModel.simprintsBiometricSearchNavigation.test {
                val navigation =
                    viewModel.onSimprintsParameterSaved(
                        uid = "biometric",
                        value = "guid-1",
                        searchItems = listOf(identifyField(value = "guid-1")),
                        initialProgramUid = "program-uid",
                        queryData = mapOf("biometric" to listOf("guid-1")),
                    )

                assertNull(navigation)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSimprintsParameterSaved should request Simprints biometric search when result is not auto open eligible`() =
        runTest {
            val viewModel =
                SimprintsSearchViewModel(
                    resolveConfirmIdentityCallout = resolveConfirmIdentityCallout,
                    sessionRepository = sessionRepository,
                    resolveSingleBiometricSearchNavigation = resolveSingleBiometricSearchNavigation,
                )
            viewModel.onSimprintsBiometricIdentificationResult(
                uid = "biometric",
                value = "guid-1",
                hasAutoOpenEligibleSimprintsIdentification = false,
            )

            viewModel.simprintsBiometricSearchNavigation.test {
                val navigation =
                    viewModel.onSimprintsParameterSaved(
                        uid = "biometric",
                        value = "guid-1",
                        searchItems = listOf(identifyField(value = "guid-1")),
                        initialProgramUid = "program-uid",
                        queryData = mapOf("biometric" to listOf("guid-1")),
                    )

                assertNull(navigation)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun identifyField(value: String?) =
        FieldUiModelImpl(
            uid = "biometric",
            label = "Biometric",
            value = value,
            displayName = value,
            autocompleteList = emptyList(),
            optionSetConfiguration = null,
            valueType = ValueType.TEXT,
            customIntent = identifyIntent(),
        )

    private fun textField(
        uid: String,
        value: String?,
    ) = FieldUiModelImpl(
        uid = uid,
        label = uid,
        value = value,
        displayName = value,
        autocompleteList = emptyList(),
        optionSetConfiguration = null,
        valueType = ValueType.TEXT,
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
