package org.dhis2.commons.simprints.usecases

import android.os.Bundle
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import timber.log.Timber

class SimprintsExtractIdentificationMatchesUseCase {
    data class SimprintsIdentificationMatch(
        val guid: String,
        val confidence: Float? = null,
    )

    operator fun invoke(extras: Bundle?): List<SimprintsIdentificationMatch> =
        extras?.getString(SIMPRINTS_IDENTIFICATION_EXTRA_NAME)?.takeIf(String::isNotBlank)
            ?.let { jsonString ->
                parseJsonElementOrNull(jsonString)
                    ?.let(::extractMatches)
                    .orEmpty()
                    .distinctBy(SimprintsIdentificationMatch::guid)
            }
            ?: emptyList()

    private fun parseJsonElementOrNull(jsonString: String): JsonElement? =
        try {
            JsonParser.parseString(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse JSON element in Simprints identification response: $jsonString")
            null
        }

    private fun extractMatches(element: JsonElement): List<SimprintsIdentificationMatch> =
        when {
            element.isJsonArray ->
                element.asJsonArray.flatMap { child ->
                    extractMatches(child)
                }

            element.isJsonObject ->
                element.asJsonObject.asMatchOrNull()?.let(::listOf)
                    ?: element.asJsonObject.entrySet()
                        .flatMap { (_, value) -> extractMatches(value) }

            else -> emptyList()
        }

    private fun JsonObject.asMatchOrNull(): SimprintsIdentificationMatch? {
        val guid =
            get(SIMPRINTS_GUID_KEY)
                ?.takeIf { !it.isJsonNull }
                ?.asString
                ?.takeIf(String::isNotBlank)
                ?: return null

        val confidence =
            get(SIMPRINTS_CONFIDENCE_KEY)
                ?.takeIf { !it.isJsonNull }
                ?.let { value ->
                    runCatching { value.asFloat }.getOrNull()
                }

        return SimprintsIdentificationMatch(
            guid = guid,
            confidence = confidence,
        )
    }

    private companion object {
        private const val SIMPRINTS_IDENTIFICATION_EXTRA_NAME = "identification"
        private const val SIMPRINTS_GUID_KEY = "guid"
        private const val SIMPRINTS_CONFIDENCE_KEY = "confidence"
    }
}
