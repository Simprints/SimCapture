package org.dhis2.simprints

import org.dhis2.commons.filters.sorting.SortingItem
import org.dhis2.data.search.SearchParametersModel
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.dhis2.usescases.searchTrackEntity.SearchTeiModel

class SimprintsLoadPossibleDuplicatesSearchResultsUseCase(
    private val searchRepository: SearchRepository,
    private val searchRepositoryKt: SearchRepositoryKt,
) {
    suspend operator fun invoke(
        searchParametersModel: SearchParametersModel,
        isOnline: Boolean,
        offlineOnly: Boolean,
        sortingItem: SortingItem?,
    ): List<SearchTeiModel>? {
        val simprintsQueryData = searchParametersModel.queryData.orEmpty()
        val simprintsQueryEntry =
            simprintsQueryData.entries.firstOrNull { (_, values) ->
                (values?.size ?: 0) > 1
            } ?: simprintsQueryData.entries.firstOrNull { (_, values) ->
                !values.isNullOrEmpty()
            } ?: return null

        val simprintsQueryValues =
            simprintsQueryEntry
                .value
                .orEmpty()
                .filter(String::isNotBlank)
                .distinct()
                .takeIf(List<String>::isNotEmpty)
                ?: return null

        val searchItems =
            buildList {
                simprintsQueryValues.forEach { guidValue ->
                    addAll(
                        searchRepositoryKt.searchTrackedEntitiesImmediate(
                            searchParametersModel =
                                searchParametersModel.copy(
                                    queryData =
                                        simprintsQueryData.toMutableMap().apply {
                                            put(simprintsQueryEntry.key, listOf(guidValue))
                                        },
                                ),
                            isOnline = isOnline,
                        ),
                    )
                }
            }.distinctBy { it.uid() }

        return searchItems.map { searchItem ->
            searchRepository.transform(
                searchItem,
                searchParametersModel.selectedProgram,
                offlineOnly,
                sortingItem,
            )
        }
    }
}

