package org.dhis2.commons.simprints.usecases

import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.mobile.commons.customintents.CustomIntentRepository
import org.dhis2.mobile.commons.model.CustomIntentActionTypeModel

class SimprintsResolvePendingEnrollmentActionUseCase(
    private val simprintsD2Repository: SimprintsD2Repository,
    private val customIntentRepository: CustomIntentRepository,
) {
    data class PendingEnrollmentAction(
        val fieldUid: String,
        val callout: SimprintsIntentUtils.PreparedCallout,
    )

    suspend operator fun invoke(
        enrollmentUid: String,
        sessionId: String,
    ): PendingEnrollmentAction? {
        val enrollment =
            simprintsD2Repository.getEnrollmentContext(enrollmentUid)
                ?: return null
        return enrollment
            .attributeUids()
            .firstNotNullOfOrNull { attributeUid ->
                resolvePendingEnrollmentAction(
                    attributeUid = attributeUid,
                    teiUid = enrollment.teiUid,
                    orgUnitUid = enrollment.orgUnitUid,
                    sessionId = sessionId,
                )
            }
    }

    private suspend fun SimprintsD2Repository.EnrollmentContext.attributeUids(): List<String> =
        buildList {
            programUid?.let { addAll(simprintsD2Repository.getProgramAttributeUids(it)) }
            addAll(simprintsD2Repository.getTrackedEntityTypeAttributeUids(teiUid))
        }

    private suspend fun resolvePendingEnrollmentAction(
        attributeUid: String,
        teiUid: String,
        orgUnitUid: String?,
        sessionId: String,
    ): PendingEnrollmentAction? {
        val customIntent =
            customIntentRepository.getCustomIntent(
                attributeUid,
                orgUnitUid,
                CustomIntentActionTypeModel.DATA_ENTRY,
            ) ?: return null
        if (!SimprintsIntentUtils.isRegisterCallout(customIntent)) {
            return null
        }
        if (!simprintsD2Repository
                .getTrackedEntityAttributeValue(teiUid, attributeUid)
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
