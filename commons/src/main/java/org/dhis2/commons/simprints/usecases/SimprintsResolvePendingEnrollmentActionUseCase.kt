package org.dhis2.commons.simprints.usecases

import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhis2.mobile.commons.customintents.CustomIntentRepository
import org.dhis2.mobile.commons.model.CustomIntentActionTypeModel

class SimprintsResolvePendingEnrollmentActionUseCase(
    private val simprintsD2Repository: SimprintsD2Repository,
    private val customIntentRepository: CustomIntentRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    data class PendingEnrollmentAction(
        val fieldUid: String,
        val callout: SimprintsIntentUtils.PreparedCallout,
    )

    suspend operator fun invoke(
        enrollmentUid: String,
        sessionId: String,
    ): PendingEnrollmentAction? =
        withContext(ioDispatcher) {
            val enrollment =
                simprintsD2Repository.getEnrollmentContext(enrollmentUid)
                    ?: return@withContext null
            enrollment.attributeUids()
                .firstNotNullOfOrNull { attributeUid ->
                    resolvePendingEnrollmentAction(
                        attributeUid = attributeUid,
                        teiUid = enrollment.teiUid,
                        orgUnitUid = enrollment.orgUnitUid,
                        sessionId = sessionId,
                    )
                }
        }

    private fun SimprintsD2Repository.EnrollmentContext.attributeUids(): List<String> =
        buildList {
            programUid?.let { addAll(simprintsD2Repository.getProgramAttributeUids(it)) }
            addAll(simprintsD2Repository.getTrackedEntityTypeAttributeUids(teiUid))
        }

    private fun resolvePendingEnrollmentAction(
        attributeUid: String,
        teiUid: String,
        orgUnitUid: String?,
        sessionId: String,
    ): PendingEnrollmentAction? {
        val customIntent = customIntentRepository.getCustomIntent(
            attributeUid,
            orgUnitUid,
            CustomIntentActionTypeModel.DATA_ENTRY,
        ) ?: return null
        if (!SimprintsIntentUtils.supportsRegisterLast(customIntent)) {
            return null
        }
        if (!simprintsD2Repository.getTrackedEntityAttributeValue(teiUid, attributeUid)
                .isNullOrBlank()
        ) {
            return null
        }

        return PendingEnrollmentAction(
            fieldUid = attributeUid,
            callout = SimprintsIntentUtils.prepareRegisterLastCallout(customIntent, sessionId),
        )
    }
}
