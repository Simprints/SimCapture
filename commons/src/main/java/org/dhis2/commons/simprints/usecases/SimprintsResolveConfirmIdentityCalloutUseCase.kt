package org.dhis2.commons.simprints.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils

class SimprintsResolveConfirmIdentityCalloutUseCase(
    private val simprintsD2Repository: SimprintsD2Repository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(
        teiUid: String,
        searchFields: Iterable<SimprintsSearchUtils.SearchField>,
        sessionId: String,
    ): SimprintsIntentUtils.PreparedCallout? =
        withContext(ioDispatcher) {
            searchFields.firstNotNullOfOrNull { field ->
                val customIntent = field.customIntent
                val selectedGuid = simprintsD2Repository.getTrackedEntityAttributeValue(teiUid, field.uid)
                when {
                    field.value.isNullOrBlank() -> null
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
}
