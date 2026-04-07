package org.dhis2.commons.simprints.utils

import android.content.Intent
import android.os.Bundle
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.mobile.commons.model.CustomIntentRequestArgumentModel
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel

object SimprintsIntentUtils {
    private const val SIMPRINTS_PACKAGE_NAME = "com.simprints.id"
    private const val SIMPRINTS_IDENTIFY_ACTION = "$SIMPRINTS_PACKAGE_NAME.IDENTIFY"
    private const val SIMPRINTS_CONFIRM_IDENTITY_ACTION = "$SIMPRINTS_PACKAGE_NAME.CONFIRM_IDENTITY"
    private const val SIMPRINTS_REGISTER_LAST_ACTION = "$SIMPRINTS_PACKAGE_NAME.REGISTER_LAST_BIOMETRICS"
    private const val SIMPRINTS_SESSION_ID_KEY = "sessionId"
    private const val SIMPRINTS_SELECTED_GUID_KEY = "selectedGuid"

    data class PreparedCallout(
        val launchIntent: Intent,
        val responseData: List<CustomIntentResponseDataModel>?,
    )

    fun isCallout(customIntent: CustomIntentModel?): Boolean = customIntent?.packageName?.startsWith(SIMPRINTS_PACKAGE_NAME) == true

    fun isIdentifyCallout(customIntent: CustomIntentModel?): Boolean = customIntent?.packageName == SIMPRINTS_IDENTIFY_ACTION

    fun supportsRegisterLast(customIntent: CustomIntentModel?): Boolean = isCallout(customIntent) && !isIdentifyCallout(customIntent)

    fun extractSessionId(extras: Bundle?): String? = extras?.getString(SIMPRINTS_SESSION_ID_KEY)

    fun prepareCallout(customIntent: CustomIntentModel): PreparedCallout = prepareCallout(customIntent, customIntent.packageName)

    fun prepareRegisterLastCallout(
        customIntent: CustomIntentModel,
        sessionId: String,
    ): PreparedCallout =
        prepareCallout(
            customIntent = customIntent,
            action = SIMPRINTS_REGISTER_LAST_ACTION,
            requestArguments =
                arrayOf(
                    requestArgument(SIMPRINTS_SESSION_ID_KEY, sessionId),
                ),
        )

    fun prepareConfirmIdentityCallout(
        customIntent: CustomIntentModel,
        sessionId: String,
        selectedGuid: String,
    ): PreparedCallout =
        prepareCallout(
            customIntent = customIntent,
            action = SIMPRINTS_CONFIRM_IDENTITY_ACTION,
            requestArguments =
                arrayOf(
                    requestArgument(SIMPRINTS_SESSION_ID_KEY, sessionId),
                    requestArgument(SIMPRINTS_SELECTED_GUID_KEY, selectedGuid),
                ),
        )

    fun hasPendingValue(
        customIntent: CustomIntentModel?,
        value: String?,
        hasPendingEnrollment: Boolean,
    ): Boolean =
        value.isNullOrEmpty() &&
            supportsRegisterLast(customIntent) &&
            hasPendingEnrollment

    fun getDisplayValues(
        value: String?,
        hasPendingValue: Boolean,
        placeholderValue: String,
    ): List<String> =
        when {
            !value.isNullOrEmpty() -> value.split(",")
            hasPendingValue -> listOf(placeholderValue)
            else -> emptyList()
        }

    private fun prepareCallout(
        customIntent: CustomIntentModel,
        action: String,
        requestArguments: Array<out CustomIntentRequestArgumentModel> = emptyArray(),
    ): PreparedCallout =
        PreparedCallout(
            launchIntent =
                Intent(action).apply {
                    customIntent.customIntentRequest
                        .filterNot { argument ->
                            argument.key in requestArguments.map { it.key }
                        }.plus(requestArguments)
                        .forEach { argument ->
                            putRequestArgumentExtra(argument)
                        }
                },
            responseData = customIntent.customIntentResponse,
        )

    private fun requestArgument(
        key: String,
        value: String,
    ): CustomIntentRequestArgumentModel = CustomIntentRequestArgumentModel(key = key, value = value)

    private fun Intent.putRequestArgumentExtra(argument: CustomIntentRequestArgumentModel) {
        when (val value = argument.value) {
            is String -> putExtra(argument.key, value)
            is Int -> putExtra(argument.key, value)
            is Double -> putExtra(argument.key, value)
            is Long -> putExtra(argument.key, value)
            is Short -> putExtra(argument.key, value)
            is Boolean -> putExtra(argument.key, value)
            else -> putExtra(argument.key, value.toString())
        }
    }
}
