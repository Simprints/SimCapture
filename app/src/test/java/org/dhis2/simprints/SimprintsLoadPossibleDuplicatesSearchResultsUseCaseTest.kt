package org.dhis2.simprints

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.dhis2.data.search.SearchParametersModel
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.dhis2.usescases.searchTrackEntity.SearchTeiModel
import org.hisp.dhis.android.core.trackedentity.search.TrackedEntitySearchItem
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
    private val searchRepository: SearchRepository = mock()
    private val searchRepositoryKt: SearchRepositoryKt = mock()

    private val useCase =
        SimprintsLoadPossibleDuplicatesSearchResultsUseCase(
            searchRepository = searchRepository,
            searchRepositoryKt = searchRepositoryKt,
        )

    @Test
    fun `invoke should return null when there is no query data`() =
        runTest {
            val result =
                useCase(
                    searchParametersModel = SearchParametersModel(selectedProgram = null, queryData = null),
                    isOnline = true,
                    offlineOnly = false,
                    sortingItem = null,
                )

            assertNull(result)
        }

    @Test
    fun `invoke should return null when query contains only blank values`() =
        runTest {
            val result =
                useCase(
                    searchParametersModel =
                        SearchParametersModel(
                            selectedProgram = null,
                            queryData = mutableMapOf("biometric" to listOf(" ", "")),
                        ),
                    isOnline = true,
                    offlineOnly = false,
                    sortingItem = null,
                )

            assertNull(result)
        }

    @Test
    fun `invoke should return transformed possible duplicates results in order`() =
        runTest {
            val item1 =
                mock<TrackedEntitySearchItem> {
                    on { uid() } doReturn "tei-1"
                }
            val item2 =
                mock<TrackedEntitySearchItem> {
                    on { uid() } doReturn "tei-2"
                }

            whenever(searchRepositoryKt.searchTrackedEntitiesImmediate(any(), any())).thenAnswer {
                val params = it.arguments[0] as SearchParametersModel
                when (params.queryData?.get("biometric")?.firstOrNull()) {
                    "guid-1" -> listOf(item1, item2)
                    "guid-2" -> listOf(item1)
                    else -> emptyList()
                }
            }

            val model1 = searchTeiModel("model-1")
            val model2 = searchTeiModel("model-2")
            whenever(searchRepository.transform(item1, null, false, null)) doReturn model1
            whenever(searchRepository.transform(item2, null, false, null)) doReturn model2

            val result =
                useCase(
                    searchParametersModel =
                        SearchParametersModel(
                            selectedProgram = null,
                            queryData = mutableMapOf("biometric" to listOf("guid-1", "guid-2")),
                        ),
                    isOnline = true,
                    offlineOnly = false,
                    sortingItem = null,
                )

            assertEquals(listOf(model1, model2), result)
            verify(searchRepositoryKt, times(2)).searchTrackedEntitiesImmediate(any(), any())
            verify(searchRepository).transform(item1, null, false, null)
            verify(searchRepository).transform(item2, null, false, null)
        }

    private fun searchTeiModel(header: String) =
        SearchTeiModel().apply {
            setHeader(header)
        }
}

