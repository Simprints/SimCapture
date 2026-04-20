package org.dhis2.form.simprints

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.form.R
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.ui.customintent.CustomIntentActivityResultContract

@Composable
fun rememberSimprintsCustomIntentFormPresenter(
    fieldUiModel: FieldUiModel,
    resources: ResourceManager,
    sessionRepository: SimprintsSessionRepository,
): SimprintsCustomIntentFormPresenter {
    val customIntent = fieldUiModel.customIntent
    val hasPendingValue =
        SimprintsIntentUtils.hasPendingValue(
            customIntent = customIntent,
            value = fieldUiModel.value,
            hasPendingEnrollment = sessionRepository.hasPendingEnrollment(),
        )
    val callout =
        remember(customIntent) {
            customIntent
                ?.takeIf(SimprintsIntentUtils::isCallout)
                ?.let(SimprintsIntentUtils::prepareCallout)
        }
    val placeholderValue =
        if (sessionRepository.hasPendingEnrollmentFromPossibleDuplicates()) {
            resources.getString(R.string.simprints_using_last_biometrics)
        } else {
            resources.getString(R.string.simprints_from_last_biometric_search)
        }

    return remember(
        fieldUiModel.value,
        callout,
        sessionRepository,
        hasPendingValue,
        placeholderValue,
    ) {
        SimprintsCustomIntentFormPresenter(
            fieldValue = fieldUiModel.value,
            callout = callout,
            capturesSessionId = SimprintsIntentUtils.isIdentifyCallout(customIntent),
            sessionRepository = sessionRepository,
            contract = CustomIntentActivityResultContract(),
            placeholderValue = placeholderValue,
            hasPendingValue = hasPendingValue,
        )
    }
}
