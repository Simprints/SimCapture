package org.dhis2.simprints

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.dhis2.commons.filters.FilterManager
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.dhis2.tracker.search.domain.SearchTrackedEntities
import org.dhis2.tracker.search.model.QueryData
import org.dhis2.tracker.search.model.SearchTrackedEntitiesInput

class SimprintsResolveSingleBiometricSearchNavigationUseCase(
    private val searchRepository: SearchRepository,
    private val searchRepositoryKt: SearchRepositoryKt,
    private val searchTrackedEntities: SearchTrackedEntities,
    private val networkUtils: NetworkUtils,
    private val filterManager: FilterManager,
    private val ioDispatcher: CoroutineDispatcher,
) {
    data class NavigationTarget(
        val teiUid: String,
        val programUid: String?,
        val enrollmentUid: String?,
        val isOnline: Boolean,
    )

    suspend operator fun invoke(
        initialProgramUid: String?,
        queryData: Map<String, List<String>?>,
        value: String?,
    ): NavigationTarget? =
        withContext(ioDispatcher) {
            val biometricSearchValues =
                value
                    ?.split(",")
                    ?.map(String::trim)
                    ?.filter(String::isNotEmpty)
                    ?: return@withContext null

            if (biometricSearchValues.size != 1) {
                return@withContext null
            }

            val isOnline = queryData.isNotEmpty() && networkUtils.isOnline()
            val selectedProgram = searchRepository.getProgram(initialProgramUid)
            val trackedEntity =
                searchTrackedEntities
                    .invokeImmediate(
                        SearchTrackedEntitiesInput(
                            selectedProgram = selectedProgram?.uid(),
                            allowCache = false,
                            excludeValues = searchRepositoryKt.getExcludeValues(),
                            hasStateFilters = filterManager.stateFilters.isNotEmpty(),
                            isOnline = isOnline,
                            queryDataList =
                                queryData.map { (attributeId, values) ->
                                    QueryData(
                                        attributeId = attributeId,
                                        values = values,
                                        searchOperator = null,
                                    )
                                },
                        ),
                    ).getOrThrow()
                    .singleOrNull() ?: return@withContext null

            val searchTeiModel =
                searchRepositoryKt.mapTrackedEntitySearchItemResultToSearchTeiModel(
                    trackedEntity,
                    filterManager.sortingItem,
                )

            NavigationTarget(
                teiUid = searchTeiModel.tei.uid,
                programUid = searchTeiModel.selectedEnrollment?.program ?: initialProgramUid,
                enrollmentUid = searchTeiModel.selectedEnrollment?.uid,
                isOnline = searchTeiModel.tei.isOnline,
            )
        }
}
