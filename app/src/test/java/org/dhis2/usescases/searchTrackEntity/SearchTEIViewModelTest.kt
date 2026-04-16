package org.dhis2.usescases.searchTrackEntity

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Map
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.dhis2.R
import org.dhis2.commons.filters.FilterManager
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.simprints.usecases.SimprintsOrderSearchResultsByIdentifyResponseUseCase
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.data.search.SearchParametersModel
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.model.FieldUiModelImpl
import org.dhis2.form.ui.intent.FormIntent
import org.dhis2.form.ui.provider.DisplayNameProvider
import org.dhis2.maps.geometry.mapper.EventsByProgramStage
import org.dhis2.maps.usecases.MapStyleConfiguration
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.simprints.SimprintsSearchViewModel
import org.dhis2.usescases.searchTrackEntity.listView.SearchResult.SearchResultType
import org.dhis2.utils.customviews.navigationbar.NavigationPage
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.program.Program
import org.hisp.dhis.android.core.trackedentity.TrackedEntityType
import org.hisp.dhis.android.core.trackedentity.search.TrackedEntitySearchItem
import org.hisp.dhis.mobile.ui.designsystem.component.navigationBar.NavigationBarItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.maplibre.geojson.BoundingBox
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.text.get

@OptIn(ExperimentalCoroutinesApi::class)
class SearchTEIViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: SearchTEIViewModel
    private val initialProgram = "programUid"
    private val initialQuery = mutableMapOf<String, List<String>?>()
    private val repository: SearchRepository = mock()
    private val repositoryKt: SearchRepositoryKt =
        mock {
            on { searchTrackedEntities(any(), any()) } doReturn flowOf(PagingData.empty())
        }
    private val pageConfigurator: SearchPageConfigurator = mock()
    private val mapDataRepository: MapDataRepository = mock()
    private val networkUtils: NetworkUtils = mock()
    private val mapStyleConfiguration: MapStyleConfiguration = mock()
    private val resourceManager: ResourceManager = mock()
    private val displayNameProvider: DisplayNameProvider = mock()
    private val filterManager: FilterManager = mock()
    private val simprintsSearchViewModel: SimprintsSearchViewModel = mock()
    private val orderSearchResultsByIdentifyResponse: SimprintsOrderSearchResultsByIdentifyResponseUseCase = mock()

    @ExperimentalCoroutinesApi
    private val testingDispatcher = StandardTestDispatcher()

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        Dispatchers.setMain(testingDispatcher)
        whenever(pageConfigurator.initVariables()) doReturn pageConfigurator
        setCurrentProgram(testingProgram())
        whenever(repository.canCreateInProgramWithoutSearch()) doReturn true
        whenever(repository.getTrackedEntityType()) doReturn testingTrackedEntityType()
        whenever(repository.filtersApplyOnGlobalSearch()) doReturn true
        viewModel =
            SearchTEIViewModel(
                initialProgram,
                initialQuery,
                repository,
                repositoryKt,
                pageConfigurator,
                mapDataRepository,
                networkUtils,
                object : DispatcherProvider {
                    override fun io(): CoroutineDispatcher = testingDispatcher

                    override fun computation(): CoroutineDispatcher = testingDispatcher

                    override fun ui(): CoroutineDispatcher = testingDispatcher
                },
                mapStyleConfiguration,
                resourceManager = resourceManager,
                displayNameProvider = displayNameProvider,
                filterManager = filterManager,
                simprintsSearchViewModel = simprintsSearchViewModel,
                orderSearchResultsByIdentifyResponse = orderSearchResultsByIdentifyResponse,
            )
        testingDispatcher.scheduler.advanceUntilIdle()
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Should set SearchList if displayFrontPageList is true`() {
        viewModel.setListScreen()

        val screenState = viewModel.screenState.value
        assertTrue(screenState is SearchList)
    }

    @Test
    fun `Should set SearchList if displayFrontPageList is false and can create`() {
        setCurrentProgram(testingProgram(displayFrontPageList = false))
        setAllowCreateBeforeSearch(true)
        viewModel.setListScreen()

        val screenState = viewModel.screenState.value
        assertTrue(screenState is SearchList)
    }

    @Test
    fun `Should set SearchForm if displayFrontPageList is false and can not create`() {
        setCurrentProgram(testingProgram(displayFrontPageList = false))
        setAllowCreateBeforeSearch(false)
        viewModel.setListScreen()

        val screenState = viewModel.screenState.value
        assertTrue(screenState is SearchList)
        assertTrue((screenState as SearchList).searchForm.isOpened)
    }

    @Test
    fun `Should set Map screen`() {
        viewModel.setMapScreen()

        val screenState = viewModel.screenState.value
        assertTrue(screenState?.screenState == SearchScreenState.MAP)
    }

    @Test
    fun `Should set Analytics screen`() {
        viewModel.setAnalyticsScreen()

        val screenState = viewModel.screenState.value
        assertTrue(screenState is SearchAnalytics)
    }

    @Test
    fun `Should set Search screen in portrait`() {
        viewModel.setSearchScreen()

        val screenState = viewModel.screenState.value
        assertTrue(screenState is SearchList)
        assertTrue((screenState as SearchList).searchForm.isOpened)
    }

    @Test
    fun `Should set Search screen in landscape`() {
        viewModel.setSearchScreen()

        val screenState = viewModel.screenState.value
        assertTrue(screenState is SearchList)
    }

    @Test
    fun `Should set previous screen`() {
        viewModel.setListScreen()
        viewModel.setSearchScreen()
        viewModel.setPreviousScreen()

        val screenStateA = viewModel.screenState.value
        assertTrue(screenStateA?.screenState == SearchScreenState.LIST)

        viewModel.setMapScreen()
        viewModel.setSearchScreen()
        viewModel.setPreviousScreen()

        val screenStateB = viewModel.screenState.value
        assertTrue(screenStateB?.screenState == SearchScreenState.MAP)
    }

    @Test
    fun `Should update query data`() {
        viewModel.onParameterIntent(
            FormIntent.OnSave(
                uid = "testingUid",
                value = "testingValue",
                valueType = ValueType.TEXT,
            ),
        )

        val queryData = viewModel.queryData

        assertTrue(queryData.isNotEmpty())
        assertTrue(queryData["testingUid"]?.size == 1)
        val values = queryData["testingUid"]
        assertTrue(values?.contains("testingValue") == true)
    }

    @Test
    fun `Should update query data when list of values is passed`() {
        viewModel.onParameterIntent(
            FormIntent.OnSave(
                uid = "testingUid",
                value = "testingValue,testingValue2",
                valueType = ValueType.TEXT,
            ),
        )

        val queryData = viewModel.queryData

        assertTrue(queryData.isNotEmpty())
        assertTrue(queryData.containsKey("testingUid"))
        val values = queryData["testingUid"]
        assertTrue(values?.size == 2)
        assertTrue(values?.contains("testingValue") == true)
        assertTrue(values?.contains("testingValue2") == true)
    }

    @Test
    fun `Should update query data when various list of values are passed`() {
        viewModel.onParameterIntent(
            FormIntent.OnSave(
                uid = "testingUid",
                value = "testingValue,testingValue2",
                valueType = ValueType.TEXT,
            ),
        )

        viewModel.onParameterIntent(
            FormIntent.OnSave(
                uid = "testingUid2",
                value = "testingValue,testingValue2",
                valueType = ValueType.TEXT,
            ),
        )

        val queryData = viewModel.queryData

        assertTrue(queryData.isNotEmpty())
        assertTrue(queryData.containsKey("testingUid"))
        val values1 = queryData["testingUid"]
        assertTrue(values1?.size == 2)
        assertTrue(values1?.contains("testingValue") == true)
        assertTrue(values1?.contains("testingValue2") == true)

        assertTrue(queryData.containsKey("testingUid"))
        val values2 = queryData["testingUid2"]
        assertTrue(values2?.size == 2)
        assertTrue(values2?.contains("testingValue") == true)
        assertTrue(values2?.contains("testingValue2") == true)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Should return local results LiveData if not searching and displayInList is true`() =
        runTest {
            val testingProgram = testingProgram()
            setCurrentProgram(testingProgram)

            viewModel.searchPagingData.take(1).asSnapshot()

            verify(repositoryKt).searchTrackedEntities(
                SearchParametersModel(
                    selectedProgram = testingProgram,
                    queryData = mutableMapOf(),
                ),
                false,
            )
        }

    @Test
    fun `Should return null if not searching and displayInList is false`() =
        runTest {
            val testingProgram = testingProgram(displayFrontPageList = false)
            setCurrentProgram(testingProgram)
            viewModel.searchPagingData.test {
                awaitItem()
                verify(repositoryKt, times(0)).searchTrackedEntities(
                    SearchParametersModel(
                        selectedProgram = testingProgram,
                        queryData = mutableMapOf(),
                    ),
                    true,
                )

                verify(repositoryKt, times(0)).searchTrackedEntities(
                    SearchParametersModel(
                        selectedProgram = testingProgram,
                        queryData = mutableMapOf(),
                    ),
                    false,
                )
            }
        }

    @Test
    fun `Should return empty global results if not searching`() =
        runTest {
            val result = viewModel.searchPagingData.take(1).asSnapshot()
            assertTrue(result.isEmpty())
        }

    @Test
    fun `Should return ordered Simprints biometric search results when available`() =
        runTest {
            val testingProgram = testingProgram(displayFrontPageList = false)
            setCurrentProgram(testingProgram)
            setAllowCreateBeforeSearch(false)
            whenever(networkUtils.isOnline()) doReturn true
            whenever(filterManager.stateFilters) doReturn emptyList()
            val firstItem = trackedEntitySearchItem("tei-1")
            val secondItem = trackedEntitySearchItem("tei-2")
            val firstModel = searchTeiModel("first")
            val secondModel = searchTeiModel("second")
            whenever(
                orderSearchResultsByIdentifyResponse(
                    any(),
                    anyOrNull(),
                    any<suspend () -> List<TrackedEntitySearchItem>>(),
                ),
            ) doReturn listOf(secondItem, firstItem)
            whenever(repository.transform(secondItem, testingProgram, false, null)) doReturn secondModel
            whenever(repository.transform(firstItem, testingProgram, false, null)) doReturn firstModel
            viewModel.searchParametersUiState =
                viewModel.searchParametersUiState.copy(
                    items = listOf(simprintsBiometricSearchField()),
                )
            viewModel.onParameterIntent(
                FormIntent.OnSave(
                    uid = "biometric",
                    value = "guid-1,guid-2",
                    valueType = ValueType.TEXT,
                ),
            )
            viewModel.setListScreen()
            viewModel.setSearchScreen()
            testingDispatcher.scheduler.advanceUntilIdle()

            val result = async { viewModel.searchPagingData.drop(1).take(1).asSnapshot() }
            viewModel.onSearch()
            testingDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(secondModel, firstModel), result.await())
            verify(repositoryKt, times(0)).searchTrackedEntities(any(), any())
        }

    @Test
    fun `Should fall back to regular search when Simprints ordering is not available`() =
        runTest {
            val testingProgram = testingProgram(displayFrontPageList = false)
            setCurrentProgram(testingProgram)
            setAllowCreateBeforeSearch(false)
            whenever(networkUtils.isOnline()) doReturn true
            whenever(filterManager.stateFilters) doReturn emptyList()
            val searchItem = trackedEntitySearchItem("tei-1")
            val searchModel = searchTeiModel("regular")
            whenever(
                orderSearchResultsByIdentifyResponse(
                    any(),
                    anyOrNull(),
                    any<suspend () -> List<TrackedEntitySearchItem>>(),
                ),
            ) doReturn null
            whenever(repositoryKt.searchTrackedEntities(any(), any())) doReturn
                flowOf(PagingData.from(listOf(searchItem)))
            whenever(repository.transform(searchItem, testingProgram, false, null)) doReturn searchModel
            viewModel.searchParametersUiState =
                viewModel.searchParametersUiState.copy(
                    items = listOf(simprintsBiometricSearchField()),
                )
            viewModel.onParameterIntent(
                FormIntent.OnSave(
                    uid = "biometric",
                    value = "guid-1,guid-2",
                    valueType = ValueType.TEXT,
                ),
            )
            viewModel.setListScreen()
            viewModel.setSearchScreen()
            testingDispatcher.scheduler.advanceUntilIdle()

            val result = async { viewModel.searchPagingData.drop(1).take(1).asSnapshot() }
            viewModel.onSearch()
            testingDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(searchModel), result.await())
            verify(repositoryKt).searchTrackedEntities(any(), any())
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `Should fetch map results`() {
        val trackerMapData =
            TrackerMapData(
                EventsByProgramStage("tag", mapOf()),
                mutableListOf(),
                hashMapOf(),
                BoundingBox.fromLngLats(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                ),
                mutableMapOf(),
            )
        whenever(
            mapDataRepository.getTrackerMapData(
                testingProgram(),
                viewModel.queryData,
            ),
        ) doReturn trackerMapData

        runTest {
            viewModel.fetchMapResults()
            testingDispatcher.scheduler.advanceUntilIdle()
            viewModel.mapResults.test {
                assertTrue(awaitItem() == trackerMapData)
            }
        }
    }

    @Test
    fun `Should use callback to perform min attributes warning`() =
        runTest {
            setCurrentProgram(testingProgram(displayFrontPageList = false))
            viewModel.onSearch()
            viewModel.searchParametersUiState.shouldShowMinAttributeWarning.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `Should search for list result`() {
        setCurrentProgram(testingProgram())
        viewModel.setListScreen()
        viewModel.setSearchScreen()
        viewModel.onParameterIntent(
            FormIntent.OnSave(
                uid = "testingUid",
                value = "testingValue",
                valueType = ValueType.TEXT,
            ),
        )
        viewModel.onSearch()

        assertTrue(viewModel.refreshData.value != null)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Should search for map result`() {
        whenever(
            mapDataRepository.getTrackerMapData(
                testingProgram(),
                viewModel.queryData,
            ),
        ) doReturn
            TrackerMapData(
                EventsByProgramStage("tag", mapOf()),
                mutableListOf(),
                hashMapOf(),
                BoundingBox.fromLngLats(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                ),
                mutableMapOf(),
            )
        setCurrentProgram(testingProgram())
        viewModel.setMapScreen()
        viewModel.setSearchScreen()
        viewModel.onParameterIntent(
            FormIntent.OnSave(
                uid = "testingUid",
                value = "testingValue",
                valueType = ValueType.TEXT,
            ),
        )
        viewModel.onSearch()

        testingDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.refreshData.value != null)
        verify(mapDataRepository).getTrackerMapData(
            testingProgram(),
            viewModel.queryData,
        )
    }

    @Test
    fun `Should filter query data for new program`() {
        viewModel.queryDataByProgram("programUid")
        verify(repository).filterQueryForProgram(viewModel.queryData, "programUid")
    }

    @Test
    fun `Should emit launch Simprints confirm identity navigation when opening dashboard requires callout`() =
        runTest {
            val intent: Intent = mock()
            whenever(
                simprintsSearchViewModel.onDashboardRequested(any(), any(), any(), any()),
            ) doReturn SimprintsSearchViewModel.DashboardAction.LaunchConfirmIdentity(intent)

            viewModel.simprintsNavigation.test {
                viewModel.onOpenDashboardRequested("teiUid", "programUid", "enrollmentUid")
                testingDispatcher.scheduler.advanceUntilIdle()

                val action = awaitItem()
                assertTrue(action is SimprintsNavigationAction.LaunchConfirmIdentity)
                assertEquals(intent, (action as SimprintsNavigationAction.LaunchConfirmIdentity).intent)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Should emit open dashboard navigation after Simprints confirm identity succeeds`() =
        runTest {
            whenever(simprintsSearchViewModel.onConfirmIdentityResult(RESULT_OK)) doReturn
                SimprintsSearchViewModel.PendingDashboardNavigation(
                    teiUid = "teiUid",
                    programUid = "programUid",
                    enrollmentUid = "enrollmentUid",
                )

            viewModel.simprintsNavigation.test {
                viewModel.onConfirmIdentityResult(RESULT_OK)
                testingDispatcher.scheduler.advanceUntilIdle()

                val action = awaitItem()
                assertTrue(action is SimprintsNavigationAction.OpenDashboard)
                action as SimprintsNavigationAction.OpenDashboard
                assertEquals("teiUid", action.teiUid)
                assertEquals("programUid", action.programUid)
                assertEquals("enrollmentUid", action.enrollmentUid)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Should emit error message when Simprints confirm identity setup fails`() =
        runTest {
            whenever(
                simprintsSearchViewModel.onDashboardRequested(any(), any(), any(), any()),
            ).thenThrow(RuntimeException())
            whenever(resourceManager.getString(R.string.custom_intent_error)) doReturn "Custom intent error"

            viewModel.simprintsNavigation.test {
                viewModel.onOpenDashboardRequested("teiUid", "programUid", "enrollmentUid")
                testingDispatcher.scheduler.advanceUntilIdle()

                val action = awaitItem()
                assertTrue(action is SimprintsNavigationAction.ShowMessage)
                assertEquals("Custom intent error", (action as SimprintsNavigationAction.ShowMessage).message)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Should refresh Simprints last biometrics label state`() {
        whenever(simprintsSearchViewModel.shouldUseLastBiometricsLabel(any())) doReturn true

        viewModel.refreshSimprintsUiState()

        assertTrue(viewModel.isSimprintsUseLastBiometricsLabel.value == true)
    }

    @Test
    fun `Should preserve entered search parameter values when parameters are refetched`() =
        runTest {
            whenever(repositoryKt.searchParameters(initialProgram, "teiTypeUid")) doReturn
                listOf(simprintsBiometricSearchField())
            viewModel.searchParametersUiState =
                viewModel.searchParametersUiState.copy(
                    items =
                        listOf(
                            simprintsBiometricSearchField().copy(
                                value = "guid-1",
                                displayName = "guid-1",
                            ),
                        ),
                )

            viewModel.fetchSearchParameters(initialProgram, "teiTypeUid")
            testingDispatcher.scheduler.advanceUntilIdle()

            assertEquals("guid-1", viewModel.searchParametersUiState.items.single().value)
            assertEquals("guid-1", viewModel.searchParametersUiState.items.single().displayName)
        }

    @Test
    fun `Should enroll on click`() {
        viewModel.onEnrollClick()
        testingDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.legacyInteraction.value is LegacyInteraction.OnEnrollClick)
    }

    @Test
    fun `Should add relationship`() {
        viewModel.onAddRelationship("teiUd", "relationshipTypeUid", false)
        testingDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.legacyInteraction.value is LegacyInteraction.OnAddRelationship)
    }

    @Test
    fun `Should show sync icon`() {
        viewModel.onSyncIconClick("teiUid")
        testingDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.legacyInteraction.value is LegacyInteraction.OnSyncIconClick)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Should downloadTei`() {
        viewModel.onDownloadTei("teiUid", null)
        testingDispatcher.scheduler.advanceUntilIdle()
        verify(repository).download("teiUid", null, null)
    }

    @Test
    fun `Should click on TEI`() {
        viewModel.onTeiClick("teiUid", null, true)
        testingDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.legacyInteraction.value is LegacyInteraction.OnTeiClick)
    }

    @Test
    fun `Should return no more result for displayInList true`() {
        viewModel.onDataLoaded(2)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.NO_MORE_RESULTS_OFFLINE)
        }
    }

    @Test
    fun `Should return search or create results for displayInList true`() {
        setAllowCreateBeforeSearch(true)
        viewModel.onDataLoaded(0)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.SEARCH_OR_CREATE)
        }
    }

    @Test
    fun `Should return no more results offline and not set SearchScreen for displayInList true`() {
        setAllowCreateBeforeSearch(false)
        viewModel.onDataLoaded(0)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.NO_MORE_RESULTS_OFFLINE)
        }
        assertTrue(viewModel.screenState.value !is SearchList)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Should return too many results for search`() {
        setCurrentProgram(testingProgram(maxTeiCountToReturn = 1))
        setAllowCreateBeforeSearch(false)
        performSearch()
        viewModel.onDataLoaded(2)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.TOO_MANY_RESULTS)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Should return search outside result for search`() {
        setCurrentProgram(testingProgram(maxTeiCountToReturn = 1))
        setAllowCreateBeforeSearch(false)
        whenever(
            repository.filterQueryForProgram(viewModel.queryData, null),
        ) doReturn mapOf("field" to listOf("value"))

        performSearch()
        viewModel.onDataLoaded(1)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.SEARCH_OUTSIDE)
        }
    }

    @Test
    fun `Should return unable to search outside result for search`() {
        setCurrentProgram(testingProgram(maxTeiCountToReturn = 1))
        setAllowCreateBeforeSearch(false)
        whenever(repository.filterQueryForProgram(viewModel.queryData, null)) doReturn mapOf()
        whenever(repository.trackedEntityTypeFields()) doReturn listOf("Field_1", "Field_2")

        performSearch()
        viewModel.onDataLoaded(1)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.UNABLE_SEARCH_OUTSIDE)
        }
    }

    @Test
    fun `Should return no more results for global search`() {
        setCurrentProgram(testingProgram(maxTeiCountToReturn = 1))
        setAllowCreateBeforeSearch(false)
        performSearch()
        viewModel.onDataLoaded(1, 1)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.NO_MORE_RESULTS)
        }
    }

    @Test
    fun `Should return no results for search`() {
        setCurrentProgram(testingProgram())
        setAllowCreateBeforeSearch(false)
        performSearch()
        viewModel.onDataLoaded(0, 0)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.NO_RESULTS)
        }
    }

    @Test
    fun `Should return init search`() {
        setCurrentProgram(testingProgram(displayFrontPageList = false))
        setAllowCreateBeforeSearch(false)
        viewModel.onDataLoaded(0, null)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.SEARCH)
        }
    }

    @Test
    fun `Should return no more results for global search when filter do not apply for it`() {
        setCurrentProgram(testingProgram(maxTeiCountToReturn = 1))
        setAllowCreateBeforeSearch(false)
        whenever(repository.filtersApplyOnGlobalSearch()) doReturn false
        performSearch()
        viewModel.onDataLoaded(1, 1)
        viewModel.dataResult.value?.apply {
            assertTrue(isNotEmpty())
            assertTrue(size == 1)
            assertTrue(first().type == SearchResultType.NO_MORE_RESULTS)
        }
    }

    @Test
    fun `Should close keyboard and filters`() {
        viewModel.onBackPressed(
            isPortrait = true,
            searchOrFilterIsOpen = true,
            keyBoardIsOpen = true,
            goBackCallback = { assertTrue(false) },
            closeSearchOrFilterCallback = { assertTrue(true) },
            closeKeyboardCallback = { assertTrue(true) },
        )
    }

    @Test
    fun `Should close filters`() {
        viewModel.onBackPressed(
            isPortrait = true,
            searchOrFilterIsOpen = true,
            keyBoardIsOpen = false,
            goBackCallback = { assertTrue(false) },
            closeSearchOrFilterCallback = { assertTrue(true) },
            closeKeyboardCallback = { assertTrue(false) },
        )
    }

    @Test
    fun `Should close keyboard and go back`() {
        viewModel.onBackPressed(
            isPortrait = true,
            searchOrFilterIsOpen = false,
            keyBoardIsOpen = true,
            goBackCallback = { assertTrue(true) },
            closeSearchOrFilterCallback = { assertTrue(false) },
            closeKeyboardCallback = { assertTrue(true) },
        )
    }

    @Test
    fun `Should go back`() {
        viewModel.onBackPressed(
            isPortrait = true,
            searchOrFilterIsOpen = false,
            keyBoardIsOpen = false,
            goBackCallback = { assertTrue(true) },
            closeSearchOrFilterCallback = { assertTrue(false) },
            closeKeyboardCallback = { assertTrue(false) },
        )
    }

    @Test
    fun `Should display navigation bar`() {
        viewModel.setListScreen()
        assertTrue(viewModel.canDisplayBottomNavigationBar())
        viewModel.setMapScreen()
        assertTrue(viewModel.canDisplayBottomNavigationBar())
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Should return break the glass result when downloading`() {
        whenever(
            repository.download(
                "teiUid",
                null,
                null,
            ),
        ) doReturn TeiDownloadResult.BreakTheGlassResult("teiUid", null)

        viewModel.onDownloadTei("teiUid", null)
        testingDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.downloadResult.value is TeiDownloadResult.BreakTheGlassResult)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Should enroll tei in current program`() {
        whenever(
            repository.download(
                "teiUid",
                null,
                null,
            ),
        ) doReturn TeiDownloadResult.TeiToEnroll("teiUid")

        viewModel.onDownloadTei("teiUid", null)
        testingDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.downloadResult.value == null)
        testingDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.legacyInteraction.value is LegacyInteraction.OnEnroll)
    }

    @Test
    fun `should return selected program uid and set theme`() {
        val programs =
            listOf(
                ProgramSpinnerModel("program1", "program1", false),
                ProgramSpinnerModel("program2", "program2", false),
            )

        viewModel.onProgramSelected(2, programs) {
            assertTrue(it == "program2")
        }
        verify(repository).setCurrentTheme(programs[1])
    }

    @Test
    fun `should return first program uid and set theme`() {
        val programs =
            listOf(
                ProgramSpinnerModel("program1", "program1", false),
            )

        viewModel.onProgramSelected(2, programs) {
            assertTrue(it == "program1")
        }
        verify(repository).setCurrentTheme(programs[0])
    }

    @Test
    fun `should return null uid and set theme`() {
        viewModel.onProgramSelected(0, listOf()) {
            assertTrue(it == null)
        }
        verify(repository).setCurrentTheme(null)
    }

    @Test
    fun `should return user-friendly names on search parameters fields`() {
        viewModel.searchParametersUiState = viewModel.searchParametersUiState.copy(items = getFieldUIModels())
        val expectedMap =
            mapOf(
                "uid1" to "Friendly OrgUnit Name",
                "uid2" to "Male",
                "uid3" to "21/02/2024",
                "uid4" to "21/02/2024 - 01:00",
                "uid5" to "Boolean: false",
                "uid6" to "Yes Only",
                "uid7" to "Text value",
                "uid9" to "18%",
            )

        val formattedMap = viewModel.getFriendlyQueryData()

        assertTrue(expectedMap == formattedMap)
    }

    @Test
    fun `should clear uiState when clearing data`() {
        viewModel.searchParametersUiState = viewModel.searchParametersUiState.copy(items = getFieldUIModels())
        performSearch()
        viewModel.clearQueryData()
        assert(viewModel.queryData.isEmpty())
        assert(viewModel.searchParametersUiState.items.all { it.value == null })
        assert(viewModel.searchParametersUiState.searchedItems.isEmpty())
    }

    @Test
    fun `should return date without format`() {
        viewModel.searchParametersUiState = viewModel.searchParametersUiState.copy(items = getMalformedDateFieldUIModels())
        val expectedMap =
            mapOf(
                "uid1" to "04",
            )

        val formattedMap = viewModel.getFriendlyQueryData()

        assertTrue(expectedMap == formattedMap)
    }

    @Test
    fun `when there is only one navigation item, navigation items list should be empty`() {
        // given
        val searchNavPageConfigurator: SearchPageConfigurator =
            mock {
                on { displayListView() } doReturn true
                on { displayMapView() } doReturn false
                on { displayAnalytics() } doReturn false
            }

        whenever(searchNavPageConfigurator.initVariables()) doReturn searchNavPageConfigurator
        whenever(resourceManager.getString(any())) doReturn "label"

        val viewModel =
            SearchTEIViewModel(
                initialProgramUid = initialProgram,
                initialQuery = initialQuery,
                searchRepository = repository,
                searchRepositoryKt = repositoryKt,
                searchNavPageConfigurator = searchNavPageConfigurator,
                mapDataRepository = mapDataRepository,
                networkUtils = networkUtils,
                dispatchers =
                    object : DispatcherProvider {
                        override fun io(): CoroutineDispatcher = testingDispatcher

                        override fun computation(): CoroutineDispatcher = testingDispatcher

                        override fun ui(): CoroutineDispatcher = testingDispatcher
                    },
                mapStyleConfig = mapStyleConfiguration,
                resourceManager = resourceManager,
                displayNameProvider = displayNameProvider,
                filterManager = filterManager,
                simprintsSearchViewModel = simprintsSearchViewModel,
                orderSearchResultsByIdentifyResponse = orderSearchResultsByIdentifyResponse,
            )
        testingDispatcher.scheduler.advanceUntilIdle()

        // then
        val navBarUIState = viewModel.navigationBarUIState.value
        assertTrue(navBarUIState.items.isEmpty())
    }

    @Test
    fun `when there is more than one navigation item, navigation items list should not be empty`() {
        // given
        val searchNavPageConfigurator: SearchPageConfigurator =
            mock {
                on { displayListView() } doReturn true
                on { displayMapView() } doReturn true
                on { displayAnalytics() } doReturn false
            }

        val viewModel =
            SearchTEIViewModel(
                initialProgramUid = initialProgram,
                initialQuery = initialQuery,
                searchRepository = repository,
                searchRepositoryKt = repositoryKt,
                searchNavPageConfigurator =
                    mock {
                        on { initVariables() } doReturn searchNavPageConfigurator
                    },
                mapDataRepository = mapDataRepository,
                networkUtils = networkUtils,
                dispatchers =
                    object : DispatcherProvider {
                        override fun io(): CoroutineDispatcher = testingDispatcher

                        override fun computation(): CoroutineDispatcher = testingDispatcher

                        override fun ui(): CoroutineDispatcher = testingDispatcher
                    },
                mapStyleConfig = mapStyleConfiguration,
                resourceManager =
                    mock {
                        on { getString(R.string.navigation_list_view) } doReturn "List"
                        on { getString(R.string.navigation_map_view) } doReturn "Map"
                    },
                displayNameProvider = displayNameProvider,
                filterManager = filterManager,
                simprintsSearchViewModel = simprintsSearchViewModel,
                orderSearchResultsByIdentifyResponse = orderSearchResultsByIdentifyResponse,
            )
        testingDispatcher.scheduler.advanceUntilIdle()

        // then
        val navBarUIState = viewModel.navigationBarUIState.value
        assertTrue(navBarUIState.items.isNotEmpty())
        assertTrue(
            navBarUIState.items ==
                listOf(
                    NavigationBarItem(
                        id = NavigationPage.LIST_VIEW,
                        icon = Icons.AutoMirrored.Outlined.List,
                        selectedIcon = Icons.AutoMirrored.Filled.List,
                        label = "List",
                    ),
                    NavigationBarItem(
                        id = NavigationPage.MAP_VIEW,
                        icon = Icons.Outlined.Map,
                        selectedIcon = Icons.Filled.Map,
                        label = "Map",
                    ),
                ),
        )
    }

    private fun getMalformedDateFieldUIModels(): List<FieldUiModel> =
        listOf(
            FieldUiModelImpl(
                uid = "uid1",
                label = "Date",
                value = "04",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.DATE,
            ),
        )

    private fun getFieldUIModels(): List<FieldUiModel> =
        listOf(
            FieldUiModelImpl(
                uid = "uid1",
                label = "Org Unit",
                value = "orgUnitUid",
                displayName = "Friendly OrgUnit Name",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.ORGANISATION_UNIT,
            ),
            FieldUiModelImpl(
                uid = "uid2",
                label = "Gender",
                value = "M",
                displayName = "Male",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.MULTI_TEXT,
            ),
            FieldUiModelImpl(
                uid = "uid3",
                label = "Date",
                value = "2024-02-21",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.DATE,
            ),
            FieldUiModelImpl(
                uid = "uid4",
                label = "Date and Time",
                value = "2024-02-21T01:00",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.DATETIME,
            ),
            FieldUiModelImpl(
                uid = "uid5",
                label = "Boolean",
                value = "false",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.BOOLEAN,
            ),
            FieldUiModelImpl(
                uid = "uid6",
                label = "Yes Only",
                value = "true",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.TRUE_ONLY,
            ),
            FieldUiModelImpl(
                uid = "uid7",
                label = "Text",
                value = "Text value",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.TEXT,
            ),
            FieldUiModelImpl(
                uid = "uid8",
                label = "Other field",
                value = null,
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.TEXT,
            ),
            FieldUiModelImpl(
                uid = "uid9",
                label = "Percentage",
                value = "18",
                autocompleteList = emptyList(),
                optionSetConfiguration = null,
                valueType = ValueType.PERCENTAGE,
            ),
        )

    private fun simprintsBiometricSearchField() =
        FieldUiModelImpl(
            uid = "biometric",
            label = "Biometric",
            value = null,
            autocompleteList = emptyList(),
            optionSetConfiguration = null,
            valueType = ValueType.TEXT,
            customIntent = simprintsIdentifyIntent(),
        )

    private fun simprintsIdentifyIntent() =
        CustomIntentModel(
            uid = "identify",
            name = "Identify",
            packageName = "com.simprints.id.IDENTIFY",
            customIntentRequest = emptyList(),
            customIntentResponse = emptyList(),
        )

    private fun trackedEntitySearchItem(uid: String): TrackedEntitySearchItem =
        TrackedEntitySearchItem(
            uid = uid,
            created = null,
            lastUpdated = null,
            createdAtClient = null,
            lastUpdatedAtClient = null,
            organisationUnit = "orgUnit",
            geometry = null,
            syncState = null,
            aggregatedSyncState = null,
            deleted = false,
            type = TrackedEntityType.builder().uid("teiType").build(),
            header = uid,
        )

    private fun searchTeiModel(header: String) =
        SearchTeiModel().apply {
            setHeader(header)
        }

    private fun testingProgram(
        displayFrontPageList: Boolean = true,
        minAttributesToSearch: Int = 1,
        maxTeiCountToReturn: Int? = null,
    ) = Program
        .builder()
        .uid("initialProgram")
        .displayName("programName")
        .displayFrontPageList(displayFrontPageList)
        .minAttributesRequiredToSearch(minAttributesToSearch)
        .trackedEntityType(TrackedEntityType.builder().uid("teTypeUid").build())
        .apply {
            maxTeiCountToReturn?.let {
                maxTeiCountToReturn(maxTeiCountToReturn)
            }
        }.build()

    private fun testingTrackedEntityType() =
        TrackedEntityType
            .builder()
            .uid("teiTypeUid")
            .displayName("teTypeName")
            .build()

    @ExperimentalCoroutinesApi
    private fun performSearch() {
        viewModel.onParameterIntent(
            FormIntent.OnSave(
                uid = "testingUid",
                value = "testingValue",
                valueType = ValueType.TEXT,
            ),
        )
        viewModel.setListScreen()
        viewModel.setSearchScreen()
        viewModel.onSearch()
        testingDispatcher.scheduler.advanceUntilIdle()
    }

    private fun setAllowCreateBeforeSearch(allow: Boolean) {
        whenever(
            repository.canCreateInProgramWithoutSearch(),
        ) doReturn allow
    }

    private fun setCurrentProgram(program: Program) {
        whenever(
            repository.getProgram(initialProgram),
        ) doReturn program
    }
}
