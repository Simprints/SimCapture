package org.dhis2.commons.simprints.usecases

import android.app.Activity
import android.content.Intent
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils

class SimprintsResolvePossibleDuplicatesSearchUseCase(
    private val extractIdentificationMatches: SimprintsExtractIdentificationMatchesUseCase,
    private val sessionRepository: SimprintsSessionRepository,
) {
    data class SimprintsPossibleDuplicatesSearch(
        val fieldUid: String,
        val guidValues: List<String>,
    )

    operator fun invoke(
        fieldUid: String,
        resultCode: Int,
        data: Intent?,
    ): SimprintsPossibleDuplicatesSearch? {
        if (resultCode != Activity.RESULT_OK) return null

        val sessionId = SimprintsIntentUtils.extractSessionId(data?.extras) ?: return null
        val guidValues =
            extractIdentificationMatches(data?.extras)
                .map { it.guid }
                .filter(String::isNotBlank)
                .distinct()
        if (guidValues.isEmpty()) return null

        sessionRepository.save(sessionId)

        return SimprintsPossibleDuplicatesSearch(
            fieldUid = fieldUid,
            guidValues = guidValues,
        )
    }
}
