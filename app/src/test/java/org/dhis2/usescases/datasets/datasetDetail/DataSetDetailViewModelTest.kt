package org.dhis2.usescases.datasets.datasetDetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModelStore
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DataSetDetailViewModelTest {
    @get:Rule
    val executorRule = InstantTaskExecutorRule()

    private val testingDispatcher = UnconfinedTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private val dispatcher: DispatcherProvider =
        mock {
            on { io() } doReturn testingDispatcher
        }
    private val dataSetPageConfigurator: DataSetPageConfigurator = mock()
    private val initializedConfigurator: DataSetPageConfigurator = mock()

    private lateinit var viewModel: DataSetDetailViewModel

    @After
    fun tearDown() {
        viewModelStore.clear()
        testingDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testingDispatcher)
    }

    @Test
    fun `Should init variables of page configurator`() {
        whenever(dataSetPageConfigurator.initVariables()) doReturn initializedConfigurator

        viewModel =
            DataSetDetailViewModel(
                dispatcher,
                dataSetPageConfigurator,
            ).also { viewModelStore.put("DataSetDetailViewModel", it) }
        viewModel.pageConfiguration.observeForever { result ->
            assertEquals(result, initializedConfigurator)
        }
    }
}
