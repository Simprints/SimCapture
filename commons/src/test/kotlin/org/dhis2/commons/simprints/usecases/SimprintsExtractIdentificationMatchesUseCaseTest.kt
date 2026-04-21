package org.dhis2.commons.simprints.usecases

import org.dhis2.commons.simprints.usecases.SimprintsExtractIdentificationMatchesUseCase.SimprintsIdentificationMatch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SimprintsExtractIdentificationMatchesUseCaseTest {
    private val useCase = SimprintsExtractIdentificationMatchesUseCase()

    @Test
    fun `invoke should extract matches from known identification payload key`() {
        val extras: android.os.Bundle = mock()
        whenever(extras.getString("identification"))
            .thenReturn("""[{"guid":"g1","confidence":0.9},{"guid":"g2"}]""")

        val result = useCase(extras)

        assertEquals(
            listOf(
                SimprintsIdentificationMatch(guid = "g1", confidence = 0.9f),
                SimprintsIdentificationMatch(guid = "g2", confidence = null),
            ),
            result,
        )
    }

    @Test
    fun `invoke should return empty list when payload is missing`() {
        val extras: android.os.Bundle = mock()
        whenever(extras.getString("identification")).thenReturn(null)
        whenever(extras.getString("identifications")).thenReturn(null)
        whenever(extras.keySet()).thenReturn(emptySet<String>())

        val result = useCase(extras)

        assertEquals(emptyList<SimprintsIdentificationMatch>(), result)
    }
}
