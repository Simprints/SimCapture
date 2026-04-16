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
    ): List<TrackedEntitySearchItem>? {
        val order = getIdentifyResponseOrder(searchFields, queryData) ?: return null
        return searchTrackedEntities()
            .map { trackedEntity ->
                trackedEntity to getMatchedGuid(trackedEntity, order)
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

    private suspend fun getMatchedGuid(
        searchItem: TrackedEntitySearchItem,
        order: IdentifyResponseOrder,
    ): String? {
        searchItem.attributeValues
            ?.firstOrNull { attribute ->
                attribute.attribute == order.fieldUid && attribute.value in order.orderByGuid
            }?.run { return value }

        return simprintsD2Repository
            .getTrackedEntityAttributeValue(searchItem.uid(), order.fieldUid)
            ?.takeIf { it in order.orderByGuid }
    }
}
