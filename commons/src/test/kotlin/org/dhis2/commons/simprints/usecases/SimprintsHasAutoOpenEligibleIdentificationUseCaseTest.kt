package org.dhis2.commons.simprints.usecases

import android.os.Bundle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SimprintsHasAutoOpenEligibleIdentificationUseCaseTest {
    private val useCase = SimprintsHasAutoOpenEligibleIdentificationUseCase()

    @Test
    fun `invoke should return true when nested extras contain an eligible identification`() {
        val extras =
            mock<Bundle> {
                on { keySet() } doReturn setOf("response")
                on { get("response") } doReturn
                    """{"results":[{"isLinkedToCredential":true,"isVerified":true}]}"""
            }

        assertTrue(useCase(extras))
    }

    @Test
    fun `invoke should return false when identification is explicitly not verified`() {
        val extras =
            mock<Bundle> {
                on { keySet() } doReturn setOf("response")
                on { get("response") } doReturn
                    """{"results":[{"isLinkedToCredential":true,"isVerified":false}]}"""
            }

        assertFalse(useCase(extras))
    }

    @Test
    fun `invoke should ignore null unparsable and non json extras`() {
        val extras =
            mock<Bundle> {
                on { keySet() } doReturn setOf("invalid", "blank", "count")
                on { get("invalid") } doReturn "{not-json"
                on { get("blank") } doReturn "   "
                on { get("count") } doReturn 1
            }

        assertFalse(useCase(null))
        assertFalse(useCase(extras))
    }
}
