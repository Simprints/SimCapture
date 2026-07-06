package org.dhis2.simprints

import org.dhis2.commons.filters.sorting.SortingItem
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.dhis2.usescases.searchTrackEntity.SearchTeiModel
import org.dhis2.tracker.search.domain.SearchTrackedEntities
import org.dhis2.tracker.search.model.QueryData
import org.dhis2.tracker.search.model.SearchTrackedEntitiesInput

class SimprintsLoadPossibleDuplicatesSearchResultsUseCase(
    private val searchRepositoryKt: SearchRepositoryKt,
    private val searchTrackedEntities: SearchTrackedEntities,
) {
    suspend operator fun invoke(
        queryDataList: List<QueryData>,
        searchInput: SearchTrackedEntitiesInput,
        sortingItem: SortingItem?,
    ): List<SearchTeiModel>? {
        val simprintsQueryData = queryDataList.associateBy(QueryData::attributeId)
        val simprintsQueryEntry =
            simprintsQueryData.entries.firstOrNull { (_, queryData) ->
                (queryData.values?.size ?: 0) > 1
            } ?: simprintsQueryData.entries.firstOrNull { (_, queryData) ->
                !queryData.values.isNullOrEmpty()
            } ?: return null

        val simprintsQueryValues =
            simprintsQueryEntry
                .value
                .values
                .orEmpty()
                .filter(String::isNotBlank)
                .distinct()
                .takeIf(List<String>::isNotEmpty)
                ?: return null

        val searchItems =
            buildList {
                simprintsQueryValues.forEach { guidValue ->
                    addAll(
                        searchTrackedEntities
                            .invokeImmediate(
                                searchInput.copy(
                                    queryDataList =
                                        queryDataList.map { queryData ->
                                            if (queryData.attributeId == simprintsQueryEntry.key) {
                                                queryData.copy(values = listOf(guidValue))
                                            } else {
                                                queryData
                                            }
                                        },
                                ),
                            ).getOrThrow(),
                    )
                }
            }.distinctBy { it.uid }

        return searchItems.map { trackedEntity ->
            searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(
                trackedEntity,
                sortingItem,
            )
        }
    }
}
