package org.dhis2.commons.simprints.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import org.dhis2.commons.simprints.model.ProgramSpecificSetting
import org.dhis2.commons.simprints.model.SimprintsDatastoreConfig
import org.hisp.dhis.android.core.D2
import timber.log.Timber

class SimprintsDatastoreRepository(
    private val d2: D2,
    private val gson: Gson = Gson(),
) {
    private var cachedConfig: CachedConfig? = null

    fun getConfig(): SimprintsDatastoreConfig {
        val localValue = getLocalDatastoreValue()
        val cached = cachedConfig

        if (cached != null && cached.rawValue == localValue) {
            return cached.config
        }

        return (localValue?.let(::parseRawValue) ?: SimprintsDatastoreConfig())
            .also { cachedConfig = CachedConfig(localValue, it) }
    }

    fun sync() {
        d2
            .dataStoreModule()
            .dataStoreDownloader()
            .byNamespace()
            .eq(DATASTORE_NAMESPACE)
            .blockingDownload()
    }

    fun biometricsCaptureOnlyAttributeId(programId: String?): String? {
        val normalizedProgramId = programId.trimToValue() ?: return null

        return try {
            getConfig()
                .programSpecificSettings
                .firstOrNull { it.programId == normalizedProgramId }
                ?.isBiometricsCaptureOnlyButtonEnabledForAttributeId
        } catch (exception: RuntimeException) {
            Timber.e(exception, DATASTORE_PARSE_ERROR)
            null
        }
    }

    fun externalCredentialAttributeId(programId: String?): String? {
        val normalizedProgramId = programId.trimToValue() ?: return null

        return try {
            getConfig()
                .programSpecificSettings
                .firstOrNull { it.programId == normalizedProgramId }
                ?.externalCredentialAttributeId
        } catch (exception: RuntimeException) {
            Timber.e(exception, DATASTORE_PARSE_ERROR)
            null
        }
    }

    private fun getLocalDatastoreValue(): String? =
        d2
            .dataStoreModule()
            .dataStore()
            .value(DATASTORE_NAMESPACE, DATASTORE_KEY)
            .blockingGet()
            ?.value()

    private fun parseRawValue(value: String): SimprintsDatastoreConfig =
        try {
            val root = parseDatastoreJsonElement(value).asJsonObject

            SimprintsDatastoreConfig(
                programSpecificSettings =
                    root
                        .get(PROGRAM_SPECIFIC_SETTINGS_KEY)
                        ?.parseList<ProgramSpecificSetting>()
                        .orEmpty()
                        .map { it.normalized() }
                        .filter { it.isValid() },
            )
        } catch (exception: JsonParseException) {
            Timber.e(exception, DATASTORE_PARSE_ERROR)
            SimprintsDatastoreConfig()
        } catch (exception: IllegalStateException) {
            Timber.e(exception, DATASTORE_PARSE_ERROR)
            SimprintsDatastoreConfig()
        }

    private fun JsonElement.parseDatastoreJsonElement(): JsonElement =
        if (isJsonPrimitive && asJsonPrimitive.isString) {
            parseDatastoreJsonElement(asString)
        } else {
            this
        }

    private fun parseDatastoreJsonElement(value: String): JsonElement = parseJsonWrapperElement(value) ?: parseJsonElement(value)

    private fun parseJsonElement(value: String): JsonElement = JsonParser.parseString(value).parseDatastoreJsonElement()

    private fun parseJsonWrapperElement(value: String): JsonElement? =
        value
            .jsonWrapperPayload()
            ?.let(::parseJsonElement)

    private fun String.jsonWrapperPayload(): String? {
        val wrappedValue = trim()
        val argumentsStart = wrappedValue.indexOf('(')
        val argumentsEnd = wrappedValue.lastIndexOf(')')
        if (argumentsStart <= 0 || argumentsEnd != wrappedValue.lastIndex) return null

        val assignment = wrappedValue.substring(argumentsStart + 1, argumentsEnd).split("=", limit = 2)
        return assignment
            .takeIf {
                wrappedValue.take(argumentsStart) == DATASTORE_JSON_WRAPPER_NAME &&
                    it.size == 2 &&
                    it[0].trim() == DATASTORE_JSON_WRAPPER_JSON_FIELD
            }?.get(1)
            ?.trim()
    }

    private inline fun <reified T> JsonElement.parseList(): List<T> =
        when {
            isJsonArray -> asJsonArray.mapNotNull { it.parseObject<T>() }
            isJsonObject -> listOfNotNull(parseObject<T>())
            else -> emptyList()
        }

    private inline fun <reified T> JsonElement.parseObject(): T? =
        try {
            gson.fromJson(this, T::class.java)
        } catch (exception: JsonParseException) {
            Timber.e(exception, DATASTORE_PARSE_ERROR)
            null
        } catch (exception: IllegalStateException) {
            Timber.e(exception, DATASTORE_PARSE_ERROR)
            null
        }

    private fun ProgramSpecificSetting.normalized(): ProgramSpecificSetting =
        copy(
            programId = programId.trimToValue(),
            isBiometricsCaptureOnlyButtonEnabledForAttributeId =
                isBiometricsCaptureOnlyButtonEnabledForAttributeId.trimToValue(),
            externalCredentialAttributeId = externalCredentialAttributeId.trimToValue(),
        )

    private fun String?.trimToValue(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private data class CachedConfig(
        val rawValue: String?,
        val config: SimprintsDatastoreConfig,
    )

    private companion object {
        private const val DATASTORE_NAMESPACE = "simprints"
        private const val DATASTORE_KEY = "biometrics"
        private const val PROGRAM_SPECIFIC_SETTINGS_KEY = "programSpecificSettings"
        private const val DATASTORE_JSON_WRAPPER_NAME = "JsonWrapper"
        private const val DATASTORE_JSON_WRAPPER_JSON_FIELD = "json"
        private const val DATASTORE_PARSE_ERROR = "Failed to parse Simprints datastore config"
    }
}
