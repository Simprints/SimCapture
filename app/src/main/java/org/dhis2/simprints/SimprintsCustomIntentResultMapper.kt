package org.dhis2.simprints

import android.content.Intent
import org.dhis2.form.ui.customintent.CustomIntentActivityResultContract
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel

class SimprintsCustomIntentResultMapper(
    private val contract: CustomIntentActivityResultContract = CustomIntentActivityResultContract(),
) {
    fun map(
        responseData: List<CustomIntentResponseDataModel>?,
        data: Intent?,
    ): String? =
        contract
            .mapIntentResponseData(responseData, data)
            ?.takeUnless(List<String>::isEmpty)
            ?.joinToString(separator = ",")
}
