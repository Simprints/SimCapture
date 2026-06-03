package org.dhis2.simprints

import android.app.Activity
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.usecases.SimprintsHasAutoOpenEligibleIdentificationUseCase
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import timber.log.Timber

class SimprintsMapBiometricSearchResultUseCase(
    private val sessionRepository: SimprintsSessionRepository,
    private val hasAutoOpenEligibleIdentification: SimprintsHasAutoOpenEligibleIdentificationUseCase,
    private val resultMapper: SimprintsCustomIntentResultMapper,
) {
    sealed class Result {
        data class Identification(
            val value: String,
            val hasAutoOpenEligibleIdentification: Boolean,
        ) : Result()

        object SearchDropout : Result()

        object NoMatches : Result()
    }

    operator fun invoke(
        responseDataJson: String?,
        resultCode: Int,
        data: Intent?,
        capturesSessionId: Boolean,
    ): Result {
        if (resultCode != Activity.RESULT_OK || !SimprintsIntentUtils.hasIdentificationResult(data)) {
            return Result.SearchDropout
        }

        if (capturesSessionId) {
            SimprintsIntentUtils.extractSessionId(data?.extras)?.let(sessionRepository::save)
        }

        val responseData = responseDataJson?.parseResponseData() ?: return Result.NoMatches
        val value = resultMapper.map(responseData, data) ?: return Result.NoMatches

        return Result.Identification(
            value = value,
            hasAutoOpenEligibleIdentification = hasAutoOpenEligibleIdentification(data?.extras),
        )
    }

    private fun String.parseResponseData(): List<CustomIntentResponseDataModel>? =
        try {
            Gson().fromJson<List<CustomIntentResponseDataModel>>(
                this,
                object : TypeToken<List<CustomIntentResponseDataModel>>() {}.type,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse CustomIntentResponseDataModel")
            null
        }
}
