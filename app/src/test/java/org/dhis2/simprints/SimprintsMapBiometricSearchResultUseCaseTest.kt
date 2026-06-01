package org.dhis2.simprints

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsHasAutoOpenEligibleIdentificationUseCase
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class SimprintsMapBiometricSearchResultUseCaseTest {
    private val sessionRepository: SimprintsSessionRepository = mock()
    private val useCase =
        SimprintsMapBiometricSearchResultUseCase(
            sessionRepository = sessionRepository,
            hasAutoOpenEligibleIdentification = SimprintsHasAutoOpenEligibleIdentificationUseCase(),
            resultMapper = SimprintsCustomIntentResultMapper(),
        )

    @Test
    fun `should return dropout before parsing response data or saving session when no identifications`() {
        val data =
            mock<Intent> {
                on { hasExtra("identification") } doReturn false
            }

        val result =
            useCase(
                responseDataJson = "{not-json",
                resultCode = RESULT_OK,
                data = data,
                capturesSessionId = true,
            )

        assertEquals(SimprintsMapBiometricSearchResultUseCase.Result.SearchDropout, result)
        verifyNoInteractions(sessionRepository)
    }

    @Test
    fun `should return dropout for cancelled identification`() {
        val result =
            useCase(
                responseDataJson = identificationResponseDataJson(),
                resultCode = RESULT_CANCELED,
                data = mock(),
                capturesSessionId = true,
            )

        assertEquals(SimprintsMapBiometricSearchResultUseCase.Result.SearchDropout, result)
        verifyNoInteractions(sessionRepository)
    }

    @Test
    fun `should still keep no-matches session available for enrol last`() {
        val data = identificationIntent("[]")

        val result =
            useCase(
                responseDataJson = identificationResponseDataJson(),
                resultCode = RESULT_OK,
                data = data,
                capturesSessionId = true,
            )

        assertEquals(SimprintsMapBiometricSearchResultUseCase.Result.NoMatches, result)
        verify(sessionRepository).save("session-id")
    }

    @Test
    fun `should return identification value`() {
        val result =
            useCase(
                responseDataJson = identificationResponseDataJson(),
                resultCode = RESULT_OK,
                data = identificationIntent("""[{"guid":"guid-1"}]"""),
                capturesSessionId = true,
            )

        assertTrue(result is SimprintsMapBiometricSearchResultUseCase.Result.Identification)
        result as SimprintsMapBiometricSearchResultUseCase.Result.Identification
        assertEquals("guid-1", result.value)
        verify(sessionRepository).save("session-id")
    }

    private fun identificationIntent(identification: String): Intent {
        val extras: Bundle = mock()
        whenever(extras.getString("sessionId")) doReturn "session-id"
        whenever(extras.keySet()) doReturn setOf("identification")
        whenever(extras.get("identification")) doReturn identification

        val intent: Intent = mock()
        whenever(intent.hasExtra("identification")) doReturn true
        whenever(intent.getStringExtra("identification")) doReturn identification
        whenever(intent.extras) doReturn extras
        return intent
    }

    private fun identificationResponseDataJson() =
        Gson().toJson(
            listOf(
                CustomIntentResponseDataModel(
                    name = "identification",
                    extraType = CustomIntentResponseExtraType.LIST_OF_OBJECTS,
                    key = "guid",
                ),
            ),
        )
}
