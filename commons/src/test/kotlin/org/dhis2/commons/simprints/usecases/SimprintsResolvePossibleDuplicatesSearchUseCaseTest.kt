package org.dhis2.commons.simprints.usecases

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsResolvePossibleDuplicatesSearchUseCase.SimprintsPossibleDuplicatesSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class SimprintsResolvePossibleDuplicatesSearchUseCaseTest {
    private val extractIdentificationMatches = SimprintsExtractIdentificationMatchesUseCase()
    private val sessionRepository: SimprintsSessionRepository = mock()

    private val useCase =
        SimprintsResolvePossibleDuplicatesSearchUseCase(
            extractIdentificationMatches = extractIdentificationMatches,
            sessionRepository = sessionRepository,
        )

    @Test
    fun `invoke should save session and return search when identification matches exist`() {
        val extras: Bundle = mock()
        whenever(extras.getString("sessionId")) doReturn "session-id"
        whenever(extras.getString("identification")) doReturn """[{"guid":"g1"},{"guid":"g2"}]"""
        val intent: Intent = mock()
        whenever(intent.extras) doReturn extras

        val result =
            useCase(
                fieldUid = "attribute-uid",
                resultCode = Activity.RESULT_OK,
                data = intent,
            )

        assertEquals(
            SimprintsPossibleDuplicatesSearch(
                fieldUid = "attribute-uid",
                guidValues = listOf("g1", "g2"),
            ),
            result,
        )
        verify(sessionRepository).save("session-id")
    }

    @Test
    fun `invoke should return null when result is not ok`() {
        val result =
            useCase(
                fieldUid = "attribute-uid",
                resultCode = Activity.RESULT_CANCELED,
                data = mock(),
            )

        assertNull(result)
        verifyNoInteractions(sessionRepository)
    }

    @Test
    fun `invoke should return null when session id is missing`() {
        val extras: Bundle = mock()
        whenever(extras.getString("sessionId")) doReturn null
        whenever(extras.getString("identification")) doReturn """[{"guid":"g1"}]"""
        val intent: Intent = mock()
        whenever(intent.extras) doReturn extras

        val result =
            useCase(
                fieldUid = "attribute-uid",
                resultCode = Activity.RESULT_OK,
                data = intent,
            )

        assertNull(result)
        verifyNoInteractions(sessionRepository)
    }
}
