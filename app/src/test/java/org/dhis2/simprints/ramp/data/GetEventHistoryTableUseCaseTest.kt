package org.dhis2.simprints.ramp.data

import org.dhis2.simprints.ramp.model.EventHistoryTable
import org.dhis2.simprints.ramp.model.EventHistoryTableColumn
import org.dhis2.simprints.ramp.model.EventHistoryTableSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetEventHistoryTableUseCaseTest {
    private val repository: EventHistoryTableRepository = mock()
    private val useCase = GetEventHistoryTableUseCase(repository)

    @Test
    fun `invoke should return repository table result`() {
        val table =
            EventHistoryTable(
                columns = listOf(EventHistoryTableColumn(eventUid = "event", label = "0")),
                sections = listOf(EventHistoryTableSection(title = "Section", rows = emptyList())),
            )
        whenever(repository.getTable()) doReturn table

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(table, result.getOrNull())
    }

    @Test
    fun `invoke should wrap repository failure`() {
        whenever(repository.getTable()) doThrow IllegalStateException("broken")

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("broken", result.exceptionOrNull()?.message)
    }
}
