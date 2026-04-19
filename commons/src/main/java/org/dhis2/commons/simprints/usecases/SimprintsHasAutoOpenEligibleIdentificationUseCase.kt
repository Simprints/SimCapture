package org.dhis2.commons.simprints.usecases

import android.os.Bundle
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import timber.log.Timber

class SimprintsHasAutoOpenEligibleIdentificationUseCase {
    operator fun invoke(extras: Bundle?): Boolean =
        extras
            ?.keySet()
            ?.asSequence()
            ?.mapNotNull { key -> extras.get(key) }
            ?.mapNotNull(::parseSimprintsJsonElement)
            ?.any(::containsAutoOpenEligibleIdentification)
            ?: false

    private fun parseSimprintsJsonElement(extraValue: Any): JsonElement? =
        when (extraValue) {
            is String -> extraValue.takeIf(String::isNotBlank)?.let(::parseJsonElementOrNull)
            is CharSequence -> extraValue.toString().takeIf(String::isNotBlank)?.let(::parseJsonElementOrNull)
            else -> null
        }

    private fun parseJsonElementOrNull(jsonString: String): JsonElement? =
        try {
            JsonParser.parseString(jsonString)
        } catch (e: JsonSyntaxException) {
            Timber.e("Failed to parse JSON element in Simprints identification response: $jsonString", e)
            null
        }

    private fun containsAutoOpenEligibleIdentification(element: JsonElement): Boolean =
        when {
            element.isJsonArray ->
                element.asJsonArray.any(::containsAutoOpenEligibleIdentification)

            element.isJsonObject ->
                containsAutoOpenEligibleIdentification(element.asJsonObject)

            else -> false
        }

    private fun containsAutoOpenEligibleIdentification(jsonObject: JsonObject): Boolean =
        hasAutoOpenEligibleIdentification(jsonObject) ||
            jsonObject.entrySet().any { (_, value) -> containsAutoOpenEligibleIdentification(value) }

    private fun hasAutoOpenEligibleIdentification(jsonObject: JsonObject): Boolean =
        jsonObject.booleanOrNull(KEY_IS_LINKED_TO_CREDENTIAL) == true &&
            jsonObject.booleanOrNull(KEY_IS_VERIFIED) != false

    private fun JsonObject.booleanOrNull(key: String): Boolean? =
        get(key)
            ?.takeIf(JsonElement::isJsonPrimitive)
            ?.asBoolean

    private companion object {
        private const val KEY_IS_LINKED_TO_CREDENTIAL = "isLinkedToCredential"
        private const val KEY_IS_VERIFIED = "isVerified"
    }
}
