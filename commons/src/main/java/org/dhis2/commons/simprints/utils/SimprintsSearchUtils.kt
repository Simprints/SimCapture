package org.dhis2.commons.simprints.utils

import org.dhis2.mobile.commons.model.CustomIntentModel

object SimprintsSearchUtils {

    data class SearchField(
        val uid: String,
        val value: String?,
        val customIntent: CustomIntentModel?
    )

    data class SearchState(
        val hasAnyQuery: Boolean,
        val hasBiometricIdentificationQuery: Boolean,
    ) {
        val shouldClearPendingSession: Boolean
            get() = hasAnyQuery && !hasBiometricIdentificationQuery
    }

    fun shouldUseLastBiometricsLabel(
        searchState: SearchState,
        hasPendingSession: Boolean,
    ): Boolean = searchState.hasBiometricIdentificationQuery && hasPendingSession

    fun filterQueryData(
        queryData: Map<String, List<String>?>,
        fields: Iterable<SearchField>,
    ): HashMap<String, List<String>> {
        val biometricFieldUids =
            fields.mapNotNullTo(mutableSetOf()) { field ->
                field.uid.takeIf { SimprintsIntentUtils.isIdentifyCallout(field.customIntent) }
            }

        return queryData.entries.fold(HashMap()) { filteredQueryData, (uid, values) ->
            if (uid !in biometricFieldUids && !values.isNullOrEmpty()) {
                filteredQueryData[uid] = values
            }
            filteredQueryData
        }
    }

    fun searchState(fields: Iterable<SearchField>): SearchState {
        val populatedFields = fields.filter { !it.value.isNullOrBlank() }
        return SearchState(
            hasAnyQuery = populatedFields.isNotEmpty(),
            hasBiometricIdentificationQuery =
                populatedFields.any { SimprintsIntentUtils.isIdentifyCallout(it.customIntent) },
        )
    }

}
