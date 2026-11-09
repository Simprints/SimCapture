package org.dhis2.simprints

import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.dhis2.commons.simprints.usecases.SimprintsOrderSearchResultsByIdentifyResponseUseCase
import org.dhis2.form.model.FieldUiModelImpl
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.tracker.search.domain.SearchTrackedEntities
import org.dhis2.tracker.search.model.QueryData
import org.dhis2.tracker.search.model.SearchTrackedEntitiesInput
import org.dhis2.tracker.search.model.TrackedEntitySearchItemResult
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.hisp.dhis.android.core.common.ValueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SimprintsLoadBiometricSearchResultsUseCaseTest {
    private val searchRepositoryKt: SearchRepositoryKt = mock()
    private val searchTrackedEntities: SearchTrackedEntities = mock()
    private val orderSearchResultsByIdentifyResponse: SimprintsOrderSearchResultsByIdentifyResponseUseCase =
        mock()

    private val useCase =
        SimprintsLoadBiometricSearchResultsUseCase(
            searchRepositoryKt = searchRepositoryKt,
            searchTrackedEntities = searchTrackedEntities,
            orderSearchResultsByIdentifyResponse = orderSearchResultsByIdentifyResponse,
        )

    @Test
    fun `invoke should return ordered biometric search results when available`() =
        runTest {
            val firstItem =
                trackedEntitySearchResult(
                    uid = "tei-1",
                    attributeValues = listOf(trackedEntityAttributeValue("biometric", "guid-1")),
                )
            val secondItem =
                trackedEntitySearchResult(
                    uid = "tei-2",
                    attributeValues = listOf(trackedEntityAttributeValue("biometric", "guid-2")),
                )
            val firstModel = searchTeiModel("tei-1", "first")
            val secondModel = searchTeiModel("tei-2", "second")

            whenever(
                orderSearchResultsByIdentifyResponse<TrackedEntitySearchItemResult>(
                    searchFields = any(),
                    queryData = any(),
                    searchTrackedEntities = any(),
                    getUid = any(),
                    getAttributeValue = any(),
                ),
            ) doReturn listOf(secondItem, firstItem)
            whenever(searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(secondItem, null)) doReturn secondModel
            whenever(searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(firstItem, null)) doReturn firstModel

            val result =
                useCase(
                    searchItems = listOf(simprintsBiometricSearchField()),
                    searchInput =
                        searchInput(
                            queryDataList =
                                listOf(
                                    QueryData(
                                        attributeId = "biometric",
                                        values = listOf("guid-1", "guid-2"),
                                        searchOperator = null,
                                    ),
                                ),
                        ),
                    sortingItem = null,
                )

            assertEquals(listOf(secondModel, firstModel), result?.asSnapshot())
            verify(searchRepositoryKt).mapTrackedEntitySearchItemResultToSearchTeiModel(secondItem, null)
            verify(searchRepositoryKt).mapTrackedEntitySearchItemResultToSearchTeiModel(firstItem, null)
        }

    @Test
    fun `invoke should return null when biometric ordering is not available`() =
        runTest {
            whenever(
                orderSearchResultsByIdentifyResponse<TrackedEntitySearchItemResult>(
                    searchFields = any(),
                    queryData = any(),
                    searchTrackedEntities = any(),
                    getUid = any(),
                    getAttributeValue = any(),
                ),
            ) doReturn null

            val result =
                useCase(
                    searchItems = listOf(simprintsBiometricSearchField()),
                    searchInput =
                        searchInput(
                            queryDataList =
                                listOf(
                                    QueryData(
                                        attributeId = "biometric",
                                        values = listOf("guid-1"),
                                        searchOperator = null,
                                    ),
                                ),
                        ),
                    sortingItem = null,
                )

            assertNull(result)
        }

    private fun simprintsBiometricSearchField() =
        FieldUiModelImpl(
            uid = "biometric",
            label = "Biometric",
            value = "guid-1,guid-2",
            displayName = "guid-1,guid-2",
            autocompleteList = emptyList(),
            optionSetConfiguration = null,
            valueType = ValueType.TEXT,
            customIntent =
                CustomIntentModel(
                    uid = "identify-intent",
                    name = "Identify",
                    packageName = "com.simprints.id.IDENTIFY",
                    customIntentRequest = emptyList(),
                    customIntentResponse = emptyList(),
                ),
        )

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
