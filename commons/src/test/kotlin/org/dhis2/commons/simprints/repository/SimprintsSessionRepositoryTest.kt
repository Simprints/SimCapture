package org.dhis2.commons.simprints.repository

import org.dhis2.commons.prefs.PreferenceProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimprintsSessionRepositoryTest {
    private val preferenceProvider: PreferenceProvider = mock()
    private val repository = SimprintsSessionRepository(preferenceProvider)

    @Test
    fun `save should store session id and clear pending enrollment flag`() {
        repository.save("session-id")

        verify(preferenceProvider).setValue(
            SimprintsSessionRepository.LAST_IDENTIFICATION_SESSION_ID,
            "session-id",
        )
        verify(preferenceProvider).removeValue(SimprintsSessionRepository.PENDING_ENROLL_LAST)
    }

    @Test
    fun `markPendingEnrollment should persist flag only when session exists`() {
        whenever(
            preferenceProvider.getString(
                SimprintsSessionRepository.LAST_IDENTIFICATION_SESSION_ID,
                null,
            ),
        ) doReturn "session-id"

        repository.markPendingEnrollment()

        verify(preferenceProvider).setValue(SimprintsSessionRepository.PENDING_ENROLL_LAST, true)
    }

    @Test
    fun `markPendingEnrollment should not persist flag when session is missing`() {
        whenever(
            preferenceProvider.getString(
                SimprintsSessionRepository.LAST_IDENTIFICATION_SESSION_ID,
                null,
            ),
        ) doReturn null

        repository.markPendingEnrollment()

        verify(preferenceProvider, never()).setValue(
            SimprintsSessionRepository.PENDING_ENROLL_LAST,
            true,
        )
    }

    @Test
    fun `pendingEnrollmentSessionId should return saved session only when pending flag is true`() {
        whenever(
            preferenceProvider.getString(
                SimprintsSessionRepository.LAST_IDENTIFICATION_SESSION_ID,
                null,
            ),
        ) doReturn "session-id"
        whenever(
            preferenceProvider.getBoolean(SimprintsSessionRepository.PENDING_ENROLL_LAST, false),
        ) doReturn true

        val sessionId = repository.pendingEnrollmentSessionId()

        assertEquals("session-id", sessionId)
    }

    @Test
    fun `pendingEnrollmentSessionId should return null when pending flag is false`() {
        whenever(
            preferenceProvider.getString(
                SimprintsSessionRepository.LAST_IDENTIFICATION_SESSION_ID,
                null,
            ),
        ) doReturn "session-id"
        whenever(
            preferenceProvider.getBoolean(SimprintsSessionRepository.PENDING_ENROLL_LAST, false),
        ) doReturn false

        val sessionId = repository.pendingEnrollmentSessionId()

        assertNull(sessionId)
    }

    @Test
    fun `clear should remove both session id and pending enrollment flag`() {
        repository.clear()

        verify(preferenceProvider).removeValue(SimprintsSessionRepository.LAST_IDENTIFICATION_SESSION_ID)
        verify(preferenceProvider).removeValue(SimprintsSessionRepository.PENDING_ENROLL_LAST)
    }
}
