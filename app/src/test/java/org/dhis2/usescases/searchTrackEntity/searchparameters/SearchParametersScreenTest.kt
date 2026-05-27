package org.dhis2.usescases.searchTrackEntity.searchparameters

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class SearchParametersScreenTest {
    @Test
    fun `mapPendingSimprintsSearchResult should return dropout before parsing response data or saving session`() {
        val data =
            mock<Intent> {
                on { hasExtra("identification") } doReturn false
            }
        val sessionRepository: SimprintsSessionRepository = mock()

        val result =
            mapPendingSimprintsSearchResult(
                responseDataJson = "{not-json",
                resultCode = RESULT_OK,
                data = data,
                capturesSessionId = true,
                sessionRepository = sessionRepository,
            )

        assertEquals(PendingSimprintsSearchResult.SearchDropout, result)
        verifyNoInteractions(sessionRepository)
    }

    @Test
    fun `mapPendingSimprintsSearchResult should keep no-match session available for enrol last`() {
        val bundleExtras =
            mock<Bundle> {
                on { getString("sessionId") } doReturn "session-id"
            }
        val data =
            mock<Intent> {
                on { hasExtra("identification") } doReturn true
                on { getStringExtra("identification") } doReturn "[]"
                on { extras } doReturn bundleExtras
            }
        val sessionRepository: SimprintsSessionRepository = mock()

        val result =
            mapPendingSimprintsSearchResult(
                responseDataJson = Gson().toJson(identificationResponseData()),
                resultCode = RESULT_OK,
                data = data,
                capturesSessionId = true,
                sessionRepository = sessionRepository,
            )

        assertEquals(PendingSimprintsSearchResult.NoMatches, result)
        verify(sessionRepository).save("session-id")
    }

    private fun identificationResponseData() =
        listOf(
            CustomIntentResponseDataModel(
                name = "identification",
                extraType = CustomIntentResponseExtraType.LIST_OF_OBJECTS,
                key = "guid",
            ),
        )
}
