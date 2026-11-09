package org.dhis2.commons.simprints.usecases

import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils
import org.hisp.dhis.android.core.trackedentity.search.TrackedEntitySearchItem

class SimprintsOrderSearchResultsByIdentifyResponseUseCase(
    private val simprintsD2Repository: SimprintsD2Repository,
) {
    private data class IdentifyResponseOrder(
        val fieldUid: String,
        val orderedGuids: List<String>,
    ) {
        val orderByGuid = orderedGuids.distinct().withIndex().associate { it.value to it.index }
    }

    suspend operator fun invoke(
        searchFields: Iterable<SimprintsSearchUtils.SearchField>,
        queryData: Map<String, List<String>?>?,
        searchTrackedEntities: suspend () -> List<TrackedEntitySearchItem>,
    ): List<TrackedEntitySearchItem>? =
        invoke(
            searchFields = searchFields,
            queryData = queryData,
            searchTrackedEntities = searchTrackedEntities,
            getUid = { trackedEntity ->
                trackedEntity.uid()
            },
            getAttributeValue = { trackedEntity, fieldUid ->
                trackedEntity.attributeValues
                    ?.firstOrNull { attribute ->
                        attribute.attribute == fieldUid
                    }?.value
            },
        )

    suspend operator fun <T> invoke(
        searchFields: Iterable<SimprintsSearchUtils.SearchField>,
        queryData: Map<String, List<String>?>?,
        searchTrackedEntities: suspend () -> List<T>,
        getUid: (T) -> String,
        getAttributeValue: (T, String) -> String?,
    ): List<T>? {
        val order = getIdentifyResponseOrder(searchFields, queryData) ?: return null
        return searchTrackedEntities()
            .map { trackedEntity ->
                trackedEntity to
                    getMatchedGuid(
                        searchItem = trackedEntity,
                        order = order,
                        getUid = getUid,
                        getAttributeValue = getAttributeValue,
                    )
            }.sortedBy { (_, guid) ->
                guid?.let(order.orderByGuid::get) ?: Int.MAX_VALUE
            }.map { (trackedEntity, _) ->
                trackedEntity
            }
    }

    private fun getIdentifyResponseOrder(
        searchFields: Iterable<SimprintsSearchUtils.SearchField>,
        queryData: Map<String, List<String>?>?,
    ): IdentifyResponseOrder? =
        searchFields.firstNotNullOfOrNull { field ->
            queryData
                ?.get(field.uid)
                ?.filter { it.isNotBlank() }
                ?.takeIf { values ->
                    values.size > 1 && SimprintsIntentUtils.isIdentifyCallout(field.customIntent)
                }?.let { values ->
                    IdentifyResponseOrder(fieldUid = field.uid, orderedGuids = values)
                }
        }

    private suspend fun <T> getMatchedGuid(
        searchItem: T,
        order: IdentifyResponseOrder,
        getUid: (T) -> String,
        getAttributeValue: (T, String) -> String?,
    ): String? {
        getAttributeValue(searchItem, order.fieldUid)
            ?.takeIf { it in order.orderByGuid }
            ?.let { return it }

        return simprintsD2Repository
            .getTrackedEntityAttributeValue(getUid(searchItem), order.fieldUid)
            ?.takeIf { it in order.orderByGuid }
    }
}
