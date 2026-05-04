package org.dhis2.commons.simprints.usecases

import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils

class SimprintsResolveConfirmIdentityCalloutUseCase(
    private val simprintsD2Repository: SimprintsD2Repository,
) {
    suspend operator fun invoke(
        teiUid: String,
        searchFields: Iterable<SimprintsSearchUtils.SearchField>,
        sessionId: String,
        allowBlankSearchValue: Boolean = false,
    ): SimprintsIntentUtils.PreparedCallout? =
        searchFields.firstNotNullOfOrNull { field ->
            val customIntent = field.customIntent
            val selectedGuid = simprintsD2Repository.getTrackedEntityAttributeValue(teiUid, field.uid)
            when {
                field.value.isNullOrBlank() && !allowBlankSearchValue -> null
                customIntent == null -> null
                !SimprintsIntentUtils.isIdentifyCallout(customIntent) -> null
                selectedGuid.isNullOrBlank() -> null
                else ->
                    SimprintsIntentUtils.prepareConfirmIdentityCallout(
                        customIntent = customIntent,
                        sessionId = sessionId,
                        selectedGuid = selectedGuid,
                    )
            }
        }
}
