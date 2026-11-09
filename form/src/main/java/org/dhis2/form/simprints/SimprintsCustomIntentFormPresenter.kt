package org.dhis2.form.simprints

import android.app.Activity
import android.content.Intent
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.form.ui.customintent.CustomIntentActivityResultContract

class SimprintsCustomIntentFormPresenter internal constructor(
    private val fieldValue: String?,
    private val callout: SimprintsIntentUtils.PreparedCallout?,
    private val capturesSessionId: Boolean,
    private val sessionRepository: SimprintsSessionRepository,
    private val contract: CustomIntentActivityResultContract,
    private val placeholderValue: String,
    val hasPendingValue: Boolean,
) {
    val handlesLaunch: Boolean
        get() = callout != null

    fun handleResult(
        resultCode: Int,
        data: Intent?,
    ): String? {
        if (resultCode != Activity.RESULT_OK) return null
        val returnedValue = returnedValue(data) ?: return null

        if (capturesSessionId) {
            SimprintsIntentUtils
                .extractSessionId(data?.extras)
                ?.let(sessionRepository::save)
        }

        return returnedValue
    }

    fun prepareLaunch() {
        if (handlesLaunch) {
            sessionRepository.clear()
        }
    }

    fun createLaunchIntent(): Intent? = callout?.launchIntent

    fun displayValues(): List<String> =
        SimprintsIntentUtils.getDisplayValues(
            value = fieldValue,
            hasPendingValue = hasPendingValue,
            placeholderValue = placeholderValue,
        )

    fun clearPendingValue() {
        if (hasPendingValue || handlesLaunch) {
            sessionRepository.clear()
        }
    }

    private fun returnedValue(data: Intent?): String? =
        contract
            .mapIntentResponseData(callout?.responseData, data)
            ?.takeUnless(List<String>::isEmpty)
            ?.joinToString(separator = ",")
}