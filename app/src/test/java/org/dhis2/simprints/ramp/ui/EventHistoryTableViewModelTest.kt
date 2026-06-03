package org.dhis2.simprints.ramp.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.simprints.ramp.data.GetEventHistoryTableUseCase
import org.dhis2.simprints.ramp.model.EventHistoryTable
import org.dhis2.simprints.ramp.model.EventHistoryTableColumn
import org.dhis2.simprints.ramp.model.EventHistoryTableSection
import org.dhis2.simprints.ramp.model.EventHistoryTableUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EventHistoryTableViewModelTest {
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var getEventHistoryTable: GetEventHistoryTableUseCase

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        dispatcherProvider =
            object : DispatcherProvider {
                override fun io(): CoroutineDispatcher = testDispatcher

                override fun computation(): CoroutineDispatcher = testDispatcher

                override fun ui(): CoroutineDispatcher = testDispatcher
            }
        getEventHistoryTable = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when table exists and ViewModel loads, then success state is shown`() {
        val table = table()
        whenever(getEventHistoryTable()) doReturn Result.success(table)

        val viewModel = viewModel()

        assertEquals(EventHistoryTableUiState.Loading, viewModel.uiState.value)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(EventHistoryTableUiState.Success(table), viewModel.uiState.value)
    }

    @Test
    fun `when table is missing and ViewModel loads, then empty state is shown`() {
        whenever(getEventHistoryTable()) doReturn Result.success(null)

        val viewModel = viewModel()

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(EventHistoryTableUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `when use case fails and ViewModel loads, then error state is shown`() {
        whenever(getEventHistoryTable()) doReturn Result.failure(IllegalStateException("broken"))

        val viewModel = viewModel()

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(EventHistoryTableUiState.Error("broken"), viewModel.uiState.value)
    }

    private fun viewModel(): EventHistoryTableViewModel =
        EventHistoryTableViewModel(
            getEventHistoryTable = getEventHistoryTable,
            dispatcherProvider = dispatcherProvider,
        )

    private fun table(): EventHistoryTable =
        EventHistoryTable(
            columns = listOf(EventHistoryTableColumn(eventUid = "event", label = "0")),
            sections = listOf(EventHistoryTableSection(title = "Section", rows = emptyList())),
        )
}
