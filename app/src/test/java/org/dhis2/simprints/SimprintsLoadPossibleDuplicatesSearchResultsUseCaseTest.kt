package org.dhis2.simprints

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.dhis2.tracker.search.domain.SearchTrackedEntities
import org.dhis2.tracker.search.model.QueryData
import org.dhis2.tracker.search.model.SearchTrackedEntitiesInput
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SimprintsLoadPossibleDuplicatesSearchResultsUseCaseTest {
    private val searchRepositoryKt: SearchRepositoryKt = mock()
    private val searchTrackedEntities: SearchTrackedEntities = mock()

    private val useCase =
        SimprintsLoadPossibleDuplicatesSearchResultsUseCase(
            searchRepositoryKt = searchRepositoryKt,
            searchTrackedEntities = searchTrackedEntities,
        )

    @Test
    fun `invoke should return null when there is no query data`() =
        runTest {
            val result =
                useCase(
                    queryDataList = emptyList(),
                    searchInput = searchInput(emptyList()),
                    sortingItem = null,
                )

            assertNull(result)
        }

    @Test
    fun `invoke should return null when query contains only blank values`() =
        runTest {
            val queryDataList =
                listOf(
                    QueryData(
                        attributeId = "biometric",
                        values = listOf(" ", ""),
                        searchOperator = null,
                    ),
                )

            val result =
                useCase(
                    queryDataList = queryDataList,
                    searchInput = searchInput(queryDataList),
                    sortingItem = null,
                )

            assertNull(result)
        }

    @Test
    fun `invoke should return transformed possible duplicates results in order`() =
        runTest {
            val item1 = trackedEntitySearchResult(uid = "tei-1")
            val item2 = trackedEntitySearchResult(uid = "tei-2")

            whenever(searchTrackedEntities.invokeImmediate(any())).thenAnswer { invocation ->
                val input = invocation.arguments[0] as SearchTrackedEntitiesInput
                val guidValue =
                    input
                        .queryDataList
                        ?.firstOrNull { queryData -> queryData.attributeId == "biometric" }
                        ?.values
                        ?.firstOrNull()
                when (guidValue) {
                    "guid-1" -> listOf(item1, item2)
                    "guid-2" -> listOf(item1)
                    else -> emptyList()
                }
            }

            val model1 = searchTeiModel("tei-1", "model-1")
            val model2 = searchTeiModel("tei-2", "model-2")
            whenever(searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(item1, null)) doReturn model1
            whenever(searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(item2, null)) doReturn model2

            val queryDataList =
                listOf(
                    QueryData(
                        attributeId = "biometric",
                        values = listOf("guid-1", "guid-2"),
                        searchOperator = null,
                    ),
                )

            val result =
                useCase(
                    queryDataList = queryDataList,
                    searchInput = searchInput(queryDataList),
                    sortingItem = null,
                )

            assertEquals(listOf(model1, model2), result)
            verify(searchTrackedEntities, times(2)).invokeImmediate(any())
            verify(searchRepositoryKt).mapTrackedEntitySearchItemResultToSearchTeiModel(item1, null)
            verify(searchRepositoryKt).mapTrackedEntitySearchItemResultToSearchTeiModel(item2, null)
        }

    private fun searchInput(queryDataList: List<QueryData>) =
        SearchTrackedEntitiesInput(
            selectedProgram = null,
            allowCache = true,
            excludeValues = emptySet(),
            hasStateFilters = false,
            isOnline = true,
            queryDataList = queryDataList,
        )
}
