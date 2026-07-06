package org.dhis2.usescases.datasets.datasetDetail.datasetlist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModelStore
import io.reactivex.Flowable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.dhis2.commons.filters.FilterManager
import org.dhis2.commons.matomo.Actions
import org.dhis2.commons.matomo.Categories
import org.dhis2.commons.matomo.Labels
import org.dhis2.commons.matomo.MatomoAnalyticsController
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.usescases.datasets.datasetDetail.DataSetDetailModel
import org.dhis2.usescases.datasets.datasetDetail.DataSetDetailRepository
import org.dhis2.usescases.datasets.datasetDetail.datasetList.DataSetListViewModel
import org.hisp.dhis.android.core.common.State
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class DataSetListViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: DataSetListViewModel
    private val viewModelStore = ViewModelStore()
    private val repository: DataSetDetailRepository = mock()
    private val filterManager: FilterManager = mock()
    private val matomoAnalyticsController: MatomoAnalyticsController = mock()

    private val testingDispatcher = UnconfinedTestDispatcher()

    private lateinit var filterProcessor: FlowableProcessor<FilterManager>
    private lateinit var filterManagerFlowable: Flowable<FilterManager>

    @After
    fun tearDown() {
        filterProcessor.onComplete()
        viewModelStore.clear()
        testingDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testingDispatcher)

        filterProcessor = PublishProcessor.create()
        filterManagerFlowable = Flowable.just(filterManager).startWith(filterProcessor)
        whenever(filterManager.asFlowable()) doReturn filterManagerFlowable
        whenever(repository.canWriteAny()) doReturn Flowable.just(true)
        viewModel = createViewModel()
    }

    @Test
    fun `Should get the list of dataSet`() {
        val dataSets = listOf(dummyDataSet(), dummyDataSet(), dummyDataSet())
        whenever(
            repository.dataSetGroups(any(), any(), any(), any()),
        ) doReturn Flowable.just(dataSets)
        viewModel = createViewModel()
        testingDispatcher.scheduler.advanceUntilIdle()
        assert(viewModel.datasets.value == dataSets)
    }

    @Test
    fun `Should get write permissions`() {
        whenever(repository.canWriteAny()) doReturn Flowable.just(true)
        testingDispatcher.scheduler.advanceUntilIdle()
        assert(viewModel.canWrite.value == true)
    }

    @Test
    fun `Should open dataset`() {
        val dataSet = dummyDataSet()
        viewModel.openDataSet(dataSet)
        assert(viewModel.selectedDataset.value?.peekContent() == dataSet)
    }

    @Test
    fun `Should sync dataset`() {
        val dataSet = dummyDataSet()
        viewModel.syncDataSet(dataSet)
        verify(matomoAnalyticsController).trackEvent(
            Categories.DATASET_LIST,
            Actions.SYNC_DATASET,
            Labels.CLICK,
        )
        assert(viewModel.selectedSync.value?.peekContent() == dataSet)
    }

    @Test
    fun `Should updateData`() {
        viewModel.updateData()
        verify(filterManager).publishData()
    }

    private fun createViewModel() =
        DataSetListViewModel(
            repository,
            filterManager,
            matomoAnalyticsController,
            object : DispatcherProvider {
                override fun io(): CoroutineDispatcher = testingDispatcher

                override fun computation(): CoroutineDispatcher = testingDispatcher

                override fun ui(): CoroutineDispatcher = testingDispatcher
            },
        ).also { viewModelStore.put("DataSetListViewModel", it) }

    private fun dummyDataSet() =
        DataSetDetailModel(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            State.SYNCED,
            "",
            true,
            false,
            Date(),
            "",
        )
}
