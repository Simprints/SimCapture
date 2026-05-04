package org.dhis2.simprints

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.dhis2.commons.filters.sorting.SortingItem
import org.dhis2.commons.simprints.usecases.SimprintsOrderSearchResultsByIdentifyResponseUseCase
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils
import org.dhis2.data.search.SearchParametersModel
import org.dhis2.form.model.FieldUiModel
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.dhis2.usescases.searchTrackEntity.SearchTeiModel

class SimprintsLoadBiometricSearchResultsUseCase(
    private val searchRepository: SearchRepository,
    private val searchRepositoryKt: SearchRepositoryKt,
    private val orderSearchResultsByIdentifyResponse: SimprintsOrderSearchResultsByIdentifyResponseUseCase,
) {
    suspend operator fun invoke(
        searchItems: List<FieldUiModel>,
        searchParametersModel: SearchParametersModel,
        isOnline: Boolean,
        offlineOnly: Boolean,
        sortingItem: SortingItem?,
    ): Flow<PagingData<SearchTeiModel>>? {
        val trackedEntities =
            orderSearchResultsByIdentifyResponse(
                searchFields = searchItems.toSearchFields(),
                queryData = searchParametersModel.queryData.orEmpty(),
                searchTrackedEntities = {
                    searchRepositoryKt.searchTrackedEntitiesImmediate(
                        searchParametersModel = searchParametersModel,
                        isOnline = isOnline,
                    )
                },
            ) ?: return null

        return flowOf(
            PagingData.from(
                trackedEntities.map { searchItem ->
                    searchRepository.transform(
                        searchItem,
                        searchParametersModel.selectedProgram,
                        offlineOnly,
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
}
