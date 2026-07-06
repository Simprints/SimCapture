package org.dhis2.simprints

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.dhis2.commons.filters.sorting.SortingItem
import org.dhis2.commons.simprints.usecases.SimprintsOrderSearchResultsByIdentifyResponseUseCase
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils
import org.dhis2.form.model.FieldUiModel
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.dhis2.usescases.searchTrackEntity.SearchTeiModel
import org.dhis2.tracker.search.domain.SearchTrackedEntities
import org.dhis2.tracker.search.model.SearchTrackedEntitiesInput

class SimprintsLoadBiometricSearchResultsUseCase(
    private val searchRepositoryKt: SearchRepositoryKt,
    private val searchTrackedEntities: SearchTrackedEntities,
    private val orderSearchResultsByIdentifyResponse: SimprintsOrderSearchResultsByIdentifyResponseUseCase,
) {
    suspend operator fun invoke(
        searchItems: List<FieldUiModel>,
        searchInput: SearchTrackedEntitiesInput,
        sortingItem: SortingItem?,
    ): Flow<PagingData<SearchTeiModel>>? {
        val trackedEntities =
            orderSearchResultsByIdentifyResponse(
                searchFields = searchItems.toSearchFields(),
                queryData = searchInput.queryDataList.toQueryData(),
                searchTrackedEntities = {
                    searchTrackedEntities
                        .invokeImmediate(searchInput)
                        .getOrThrow()
                },
                getUid = { trackedEntity ->
                    trackedEntity.uid
                },
                getAttributeValue = { trackedEntity, fieldUid ->
                    trackedEntity.attributeValues
                        .firstOrNull { it.attribute == fieldUid }
                        ?.value
                },
            ) ?: return null

        return flowOf(
            PagingData.from(
                trackedEntities.map { trackedEntity ->
                    searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(
                        trackedEntity,
                        sortingItem,
                    )
                },
            ),
        )
    }

    private fun List<FieldUiModel>.toSearchFields(): List<SimprintsSearchUtils.SearchField> =
        map { field ->
            SimprintsSearchUtils.SearchField(
                uid = field.uid,
                value = field.value,
                customIntent = field.customIntent,
            )
        }

    private fun List<org.dhis2.tracker.search.model.QueryData>?.toQueryData(): Map<String, List<String>?> =
        this
            ?.associate { queryData ->
                queryData.attributeId to queryData.values
            }.orEmpty()
}
