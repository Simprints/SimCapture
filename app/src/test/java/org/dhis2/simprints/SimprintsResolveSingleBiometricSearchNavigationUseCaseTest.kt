package org.dhis2.simprints

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.dhis2.commons.filters.FilterManager
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.tracker.search.domain.SearchTrackedEntities
import org.dhis2.tracker.search.model.QueryData
import org.dhis2.tracker.search.model.SearchTrackedEntitiesInput
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.program.Program
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SimprintsResolveSingleBiometricSearchNavigationUseCaseTest {
    private val searchRepository: SearchRepository = mock()
    private val searchRepositoryKt: SearchRepositoryKt = mock()
    private val searchTrackedEntities: SearchTrackedEntities = mock()
    private val networkUtils: NetworkUtils = mock()
    private val filterManager: FilterManager = mock()

    @Test
    fun `invoke should return matched enrollment navigation when a single biometric result is found`() =
        runTest {
            val program = program("initialProgramUid")
            val trackedEntity = trackedEntitySearchResult(uid = "teiUid", isOnline = true)
            val searchTeiModel =
                searchTeiModel(
                    teiUid = "teiUid",
                    selectedEnrollment =
                        domainEnrollment(
                            uid = "enrollmentUid",
                            programUid = "matchedProgramUid",
                            teiUid = "teiUid",
                        ),
                    isOnline = true,
                )
            whenever(searchRepository.getProgram("initialProgramUid")) doReturn program
            whenever(searchRepositoryKt.getExcludeValues()) doReturn HashSet()
            whenever(networkUtils.isOnline()) doReturn true
            whenever(filterManager.stateFilters) doReturn emptyList()
            doReturn(listOf(trackedEntity))
                .whenever(searchTrackedEntities)
                .invokeImmediate(
                    searchInput(
                        selectedProgram = "initialProgramUid",
                        isOnline = true,
                    ),
                )
            whenever(searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(trackedEntity, null)) doReturn
                searchTeiModel

            val result =
                useCase(StandardTestDispatcher(testScheduler))(
                    initialProgramUid = "initialProgramUid",
                    queryData = mapOf("biometric" to listOf("guid-1")),
                    value = "guid-1",
                )

            assertEquals(
                SimprintsResolveSingleBiometricSearchNavigationUseCase.NavigationTarget(
                    teiUid = "teiUid",
                    programUid = "matchedProgramUid",
                    enrollmentUid = "enrollmentUid",
                    isOnline = true,
                ),
                result,
            )
        }

    @Test
    fun `invoke should fall back to the initial program when the matched result has no selected enrollment`() =
        runTest {
            val program = program("initialProgramUid")
            val trackedEntity = trackedEntitySearchResult(uid = "teiUid", isOnline = false)
            whenever(searchRepository.getProgram("initialProgramUid")) doReturn program
            whenever(searchRepositoryKt.getExcludeValues()) doReturn HashSet()
            whenever(networkUtils.isOnline()) doReturn false
            whenever(filterManager.stateFilters) doReturn emptyList()
            doReturn(listOf(trackedEntity))
                .whenever(searchTrackedEntities)
                .invokeImmediate(
                    searchInput(
                        selectedProgram = "initialProgramUid",
                        isOnline = false,
                    ),
                )
            whenever(searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(trackedEntity, null)) doReturn
                searchTeiModel(teiUid = "teiUid", isOnline = false)

            val result =
                useCase(StandardTestDispatcher(testScheduler))(
                    initialProgramUid = "initialProgramUid",
                    queryData = mapOf("biometric" to listOf("guid-1")),
                    value = "guid-1",
                )

            assertEquals(
                SimprintsResolveSingleBiometricSearchNavigationUseCase.NavigationTarget(
                    teiUid = "teiUid",
                    programUid = "initialProgramUid",
                    enrollmentUid = null,
                    isOnline = false,
                ),
                result,
            )
        }

    @Test
    fun `invoke should return null when biometric value does not resolve to exactly one identifier`() =
        runTest {
            val result =
                useCase(StandardTestDispatcher(testScheduler))(
                    initialProgramUid = "initialProgramUid",
                    queryData = mapOf("biometric" to listOf("guid-1", "guid-2")),
                    value = "guid-1,guid-2",
                )

            assertNull(result)
            verify(searchTrackedEntities, never()).invokeImmediate(any())
        }

    @Test
    fun `invoke should return null when search resolves multiple tracked entities`() =
        runTest {
            val program = program("initialProgramUid")
            whenever(searchRepository.getProgram("initialProgramUid")) doReturn program
            whenever(searchRepositoryKt.getExcludeValues()) doReturn HashSet()
            whenever(networkUtils.isOnline()) doReturn true
            whenever(filterManager.stateFilters) doReturn emptyList()
            doReturn(
                listOf(
                    trackedEntitySearchResult(uid = "tei-1"),
                    trackedEntitySearchResult(uid = "tei-2"),
                ),
            ).whenever(searchTrackedEntities)
                .invokeImmediate(
                    searchInput(
                        selectedProgram = "initialProgramUid",
                        isOnline = true,
                    ),
                )

            val result =
                useCase(StandardTestDispatcher(testScheduler))(
                    initialProgramUid = "initialProgramUid",
                    queryData = mapOf("biometric" to listOf("guid-1")),
                    value = "guid-1",
                )

            assertNull(result)
            verify(searchRepositoryKt, never()).mapTrackedEntitySearchItemResultToSearchTeiModel(any(), any())
        }

    private fun useCase(ioDispatcher: CoroutineDispatcher) =
        SimprintsResolveSingleBiometricSearchNavigationUseCase(
            searchRepository = searchRepository,
            searchRepositoryKt = searchRepositoryKt,
            searchTrackedEntities = searchTrackedEntities,
            networkUtils = networkUtils,
            filterManager = filterManager,
            ioDispatcher = ioDispatcher,
        )

    private fun program(uid: String): Program =
        Program
            .builder()
            .uid(uid)
            .categoryCombo(ObjectWithUid.create("categoryComboUid"))
            .enrollmentCategoryCombo(ObjectWithUid.create("categoryComboUid"))
            .build()

    private fun searchInput(
        selectedProgram: String,
        isOnline: Boolean,
    ) = SearchTrackedEntitiesInput(
        selectedProgram = selectedProgram,
        allowCache = false,
        excludeValues = emptySet(),
        hasStateFilters = false,
        isOnline = isOnline,
        queryDataList =
            listOf(
                QueryData(
                    attributeId = "biometric",
                    values = listOf("guid-1"),
                    searchOperator = null,
                ),
            ),
    )
}
