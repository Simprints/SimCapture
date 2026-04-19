package org.dhis2.simprints

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.dhis2.commons.filters.FilterManager
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.data.search.SearchParametersModel
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt

class SimprintsResolveSingleBiometricSearchNavigationUseCase(
    private val searchRepository: SearchRepository,
    private val searchRepositoryKt: SearchRepositoryKt,
    private val networkUtils: NetworkUtils,
    private val filterManager: FilterManager,
    private val ioDispatcher: CoroutineDispatcher,
) {
    data class NavigationTarget(
        val teiUid: String,
        val programUid: String?,
        val enrollmentUid: String?,
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

            val searchParametersModel =
                SearchParametersModel(
                    selectedProgram = searchRepository.getProgram(initialProgramUid),
                    queryData = queryData.toMutableMap(),
                )
            val isOnline = queryData.isNotEmpty() && networkUtils.isOnline()
            val trackedEntity =
                searchRepositoryKt
                    .searchTrackedEntitiesImmediate(
                        searchParametersModel = searchParametersModel,
                        isOnline = isOnline,
                    ).singleOrNull() ?: return@withContext null

            val searchTeiModel =
                searchRepository.transform(
                    trackedEntity,
                    searchParametersModel.selectedProgram,
                    !(isOnline && filterManager.stateFilters.isEmpty()),
                    filterManager.sortingItem,
                )

            NavigationTarget(
                teiUid = searchTeiModel.uid(),
                programUid = searchTeiModel.selectedEnrollment?.program() ?: initialProgramUid,
                enrollmentUid = searchTeiModel.selectedEnrollment?.uid(),
            )
        }
}
