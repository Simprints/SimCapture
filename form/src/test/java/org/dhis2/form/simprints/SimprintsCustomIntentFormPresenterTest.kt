package org.dhis2.form.simprints

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.form.ui.customintent.CustomIntentActivityResultContract
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimprintsCustomIntentFormPresenterTest {
    private val sessionRepository: SimprintsSessionRepository = mock()
    private val contract: CustomIntentActivityResultContract = mock()

    @Test
    fun `handleResult should return mapped value and save session id when requested`() {
        val responseData = responseData()
        val extras =
            mock<Bundle> {
                on { getString("sessionId") } doReturn "session-id"
            }
        val data =
            mock<Intent> {
                on { this.extras } doReturn extras
            }
        whenever(contract.mapIntentResponseData(responseData, data)) doReturn
            listOf(
                "guid-1",
                "guid-2",
            )
        val presenter =
            SimprintsCustomIntentFormPresenter(
                fieldValue = null,
                callout = preparedCallout(responseData),
                capturesSessionId = true,
                sessionRepository = sessionRepository,
                contract = contract,
                placeholderValue = "From last biometric search",
                hasPendingValue = false,
            )

        val result = presenter.handleResult(RESULT_OK, data)

        assertEquals("guid-1,guid-2", result)
        verify(sessionRepository).save("session-id")
    }

    @Test
    fun `handleResult should return null when activity result is not ok`() {
        val presenter =
            SimprintsCustomIntentFormPresenter(
                fieldValue = null,
                callout = preparedCallout(responseData()),
                capturesSessionId = true,
                sessionRepository = sessionRepository,
                contract = contract,
                placeholderValue = "From last biometric search",
                hasPendingValue = false,
            )

        val result = presenter.handleResult(resultCode = 0, data = mock())

        assertNull(result)
        verify(contract, never()).mapIntentResponseData(
            org.mockito.kotlin.anyOrNull(),
            org.mockito.kotlin.anyOrNull(),
        )
    }

    @Test
    fun `displayValues should show placeholder when pending value exists`() {
        val presenter =
            SimprintsCustomIntentFormPresenter(
                fieldValue = null,
                callout = null,
                capturesSessionId = false,
                sessionRepository = sessionRepository,
                contract = contract,
                placeholderValue = "From last biometric search",
                hasPendingValue = true,
            )

        assertEquals(listOf("From last biometric search"), presenter.displayValues())
    }

    @Test
    fun `prepareLaunch should clear session when presenter handles launch`() {
        val presenter =
            SimprintsCustomIntentFormPresenter(
                fieldValue = null,
                callout = preparedCallout(responseData()),
                capturesSessionId = false,
                sessionRepository = sessionRepository,
                contract = contract,
                placeholderValue = "From last biometric search",
                hasPendingValue = false,
            )

        presenter.prepareLaunch()

        verify(sessionRepository).clear()
    }

    @Test
    fun `prepareLaunch should clear session when pending value exists and presenter handles launch`() {
        val presenter =
            SimprintsCustomIntentFormPresenter(
                fieldValue = null,
                callout = preparedCallout(responseData()),
                capturesSessionId = false,
                sessionRepository = sessionRepository,
                contract = contract,
                placeholderValue = "From last biometric search",
                hasPendingValue = true,
            )

        presenter.prepareLaunch()

        verify(sessionRepository).clear()
    }

    @Test
    fun `clearPendingValue should clear session when pending value exists`() {
        val presenter =
            SimprintsCustomIntentFormPresenter(
                fieldValue = null,
                callout = null,
                capturesSessionId = false,
                sessionRepository = sessionRepository,
                contract = contract,
                placeholderValue = "From last biometric search",
                hasPendingValue = true,
            )

        presenter.clearPendingValue()

        verify(sessionRepository).clear()
    }

    @Test
    fun `createLaunchIntent should expose prepared launch intent`() {
        val launchIntent: Intent = mock()
        val presenter =
            SimprintsCustomIntentFormPresenter(
                fieldValue = null,
                callout =
                    SimprintsIntentUtils.PreparedCallout(
                        launchIntent = launchIntent,
                        responseData = responseData(),
                    ),
                capturesSessionId = false,
                sessionRepository = sessionRepository,
                contract = contract,
                placeholderValue = "From last biometric search",
                hasPendingValue = false,
            )

        assertTrue(presenter.handlesLaunch)
        assertEquals(launchIntent, presenter.createLaunchIntent())
    }

    private fun preparedCallout(responseData: List<CustomIntentResponseDataModel>) =
        SimprintsIntentUtils.PreparedCallout(
            launchIntent = mock(),
            responseData = responseData,
        )

    private fun responseData() =
        listOf(
            CustomIntentResponseDataModel(
                name = "guid",
                extraType = CustomIntentResponseExtraType.STRING,
                key = null,
            ),
        )
}
