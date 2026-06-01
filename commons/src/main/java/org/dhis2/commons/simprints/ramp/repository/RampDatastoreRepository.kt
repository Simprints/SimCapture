package org.dhis2.commons.simprints.ramp.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.dhis2.commons.simprints.ramp.model.DataElementHistoryChartConfig
import org.dhis2.commons.simprints.ramp.model.RampDatastoreConfig
import org.hisp.dhis.android.core.D2

class RampDatastoreRepository(
    private val d2: D2,
    private val gson: Gson = Gson(),
) {
    private var cachedConfig: CachedConfig? = null

    fun getConfig(): RampDatastoreConfig {
        val localValue = getLocalRampDatastoreValue()
        val cached = cachedConfig

        if (cached != null && cached.rawValue == localValue) {
            return cached.config
        }

        return (localValue?.let(::parseRawValue) ?: RampDatastoreConfig())
            .also { cachedConfig = CachedConfig(localValue, it) }
    }

    fun sync() {
        d2
            .dataStoreModule()
            .dataStoreDownloader()
            .byNamespace()
            .eq(RAMP_DATASTORE_NAMESPACE)
            .blockingDownload()
    }

    private fun getLocalRampDatastoreValue(): String? =
        runCatching {
            d2
                .dataStoreModule()
                .dataStore()
                .value(RAMP_DATASTORE_NAMESPACE, RAMP_DATASTORE_KEY)
                .blockingGet()
                ?.value()
        }.getOrNull()

    private fun parseRawValue(value: String): RampDatastoreConfig =
        runCatching {
            val root =
                parseDatastoreJsonElement(value)?.asJsonObject
                    ?: return@runCatching RampDatastoreConfig()
            RampDatastoreConfig(
                dataElementHistoryCharts =
                    root
                        .get(DATA_ELEMENT_HISTORY_CHARTS_KEY)
                        ?.parseList<DataElementHistoryChartConfig>()
                        .orEmpty()
                        .filter { it.isValid() },
            )
        }.getOrDefault(RampDatastoreConfig())

    private fun JsonElement.parseDatastoreJsonElement(): JsonElement? =
        runCatching {
            if (isJsonPrimitive && asJsonPrimitive.isString) {
                parseDatastoreJsonElement(asString) ?: this
            } else {
                this
            }
        }.getOrNull()

    private fun parseDatastoreJsonElement(value: String): JsonElement? = parseJsonElement(value) ?: parseJsonWrapperElement(value)

    private fun parseJsonElement(value: String): JsonElement? =
        runCatching {
            JsonParser.parseString(value).parseDatastoreJsonElement()
        }.getOrNull()

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

    private inline fun <reified T> JsonElement.parseObject(): T? = runCatching { gson.fromJson(this, T::class.java) }.getOrNull()

    private data class CachedConfig(
        val rawValue: String?,
        val config: RampDatastoreConfig,
    )

    private companion object {
        private const val RAMP_DATASTORE_NAMESPACE = "simprints"
        private const val RAMP_DATASTORE_KEY = "ramp"
        private const val DATA_ELEMENT_HISTORY_CHARTS_KEY = "dataElementHistoryCharts"
        private const val DATASTORE_JSON_WRAPPER_NAME = "JsonWrapper"
        private const val DATASTORE_JSON_WRAPPER_JSON_FIELD = "json"
    }
}
