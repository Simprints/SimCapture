package org.dhis2.commons.simprints

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import org.hisp.dhis.android.core.D2

object RampDatastoreConfig {
    private const val RAMP_DATASTORE_NAMESPACE = "simprints"
    private const val RAMP_DATASTORE_KEY = "ramp"
    private const val DATA_ELEMENT_HISTORY_CHARTS_KEY = "dataElementHistoryCharts"
    private const val PROGRAM_STAGE_FORM_CHARTS_KEY = "programStageFormCharts"
    private const val PROGRAM_STAGE_HISTORY_TABLE_KEY = "programStageHistoryTable"
    private const val DATASTORE_JSON_WRAPPER_PREFIX = "JsonWrapper(json="
    private const val DATASTORE_JSON_WRAPPER_SUFFIX = ")"

    @JvmStatic
    fun load(d2: D2): RampConfig {
        val localConfig = loadLocal(d2)

        downloadRampDatastore(d2)

        val downloadedConfig = loadLocal(d2)
        return downloadedConfig.takeIf { it.isNotEmpty() } ?: localConfig
    }

    @JvmStatic
    fun loadLocal(d2: D2): RampConfig = localRampDatastoreValue(d2)?.let(::parse) ?: RampConfig()

    @JvmStatic
    fun hasProgramStageHistoryTable(
        d2: D2,
        programId: String?,
        programStageId: String?,
    ): Boolean = programStageHistoryTableConfig(d2, programId, programStageId) != null

    @JvmStatic
    fun hasLocalProgramStageHistoryTable(
        d2: D2,
        programId: String?,
        programStageId: String?,
    ): Boolean = localProgramStageHistoryTableConfig(d2, programId, programStageId) != null

    @JvmStatic
    fun programStageHistoryTableConfig(
        d2: D2,
        programId: String?,
        programStageId: String?,
    ): ProgramStageHistoryTableConfig? =
        load(d2)
            .programStageHistoryTable
            .firstOrNull { config ->
                config.programId == programId && config.programStageId == programStageId
            }

    @JvmStatic
    fun localProgramStageHistoryTableConfig(
        d2: D2,
        programId: String?,
        programStageId: String?,
    ): ProgramStageHistoryTableConfig? =
        loadLocal(d2)
            .programStageHistoryTable
            .firstOrNull { config ->
                config.programId == programId && config.programStageId == programStageId
            }

    private fun localRampDatastoreValue(d2: D2): String? =
        runCatching {
            d2
                .dataStoreModule()
                .dataStore()
                .byNamespace()
                .eq(RAMP_DATASTORE_NAMESPACE)
                .byKey()
                .eq(RAMP_DATASTORE_KEY)
                .blockingGet()
                .firstOrNull()
                ?.value()
        }.getOrNull()

    private fun downloadRampDatastore(d2: D2) {
        runCatching {
            d2
                .dataStoreModule()
                .dataStoreDownloader()
                .byNamespace()
                .eq(RAMP_DATASTORE_NAMESPACE)
                .blockingDownload()
        }
    }

    private fun parse(value: String): RampConfig {
        return try {
            val root = parseDatastoreJsonElement(value)?.asJsonObject ?: return RampConfig()
            RampConfig(
                dataElementHistoryCharts =
                    root
                        .firstExisting(DATA_ELEMENT_HISTORY_CHARTS_KEY, PROGRAM_STAGE_FORM_CHARTS_KEY)
                        ?.parseList<DataElementHistoryChartConfig>()
                        .orEmpty()
                        .filter { it.isValid() },
                programStageHistoryTable =
                    root
                        .get(PROGRAM_STAGE_HISTORY_TABLE_KEY)
                        ?.parseList<ProgramStageHistoryTableConfig>()
                        .orEmpty()
                        .filter { it.isValid() },
            )
        } catch (_: Exception) {
            RampConfig()
        }
    }

    private fun JsonElement.parseDatastoreJsonElement(): JsonElement? =
        runCatching {
            if (isJsonPrimitive && asJsonPrimitive.isString) {
                parseDatastoreJsonElement(asString) ?: this
            } else {
                this
            }
        }.getOrNull()

    private fun parseDatastoreJsonElement(value: String): JsonElement? =
        runCatching {
            JsonParser
                .parseString(value.unwrapDatastoreJson())
                .parseDatastoreJsonElement()
        }.getOrNull()

    private fun String.unwrapDatastoreJson(): String =
        trim()
            .removePrefix(DATASTORE_JSON_WRAPPER_PREFIX)
            .removeSuffix(DATASTORE_JSON_WRAPPER_SUFFIX)

    private fun com.google.gson.JsonObject.firstExisting(vararg keys: String): JsonElement? =
        keys.firstNotNullOfOrNull { key -> get(key) }

    private inline fun <reified T> JsonElement.parseList(): List<T> =
        when {
            isJsonArray -> asJsonArray.mapNotNull { it.parseObject<T>() }
            isJsonObject -> listOfNotNull(parseObject<T>())
            else -> emptyList()
        }

    private inline fun <reified T> JsonElement.parseObject(): T? =
        try {
            Gson().fromJson(this, T::class.java)
        } catch (_: Exception) {
            null
        }
}

data class RampConfig(
    val dataElementHistoryCharts: List<DataElementHistoryChartConfig> = emptyList(),
    val programStageHistoryTable: List<ProgramStageHistoryTableConfig> = emptyList(),
) {
    fun isNotEmpty(): Boolean = dataElementHistoryCharts.isNotEmpty() || programStageHistoryTable.isNotEmpty()
}

data class DataElementHistoryChartConfig(
    @SerializedName("programId")
    val programId: String? = null,
    @SerializedName("programStageId")
    val programStageId: String? = null,
    @SerializedName("dataElementId")
    val dataElementId: String? = null,
    @SerializedName("maxHistoryLengthExcludingCurrent")
    val maxHistoryLengthExcludingCurrent: Int? = null,
    @SerializedName("showIfNoHistory")
    val showIfNoHistory: Boolean? = null,
    @SerializedName("dataPointPositionsOnChart")
    val dataPointPositionsOnChart: Int? = null,
) {
    fun isValid(): Boolean =
        !programId.isNullOrBlank() &&
            !programStageId.isNullOrBlank() &&
            !dataElementId.isNullOrBlank()
}

data class ProgramStageHistoryTableConfig(
    @SerializedName("programId")
    val programId: String? = null,
    @SerializedName("programStageId")
    val programStageId: String? = null,
    @SerializedName("dataPointColumnsInTable")
    val dataPointColumnsInTable: Int? = null,
    @SerializedName("headerVisitNumberDataElementId")
    val headerVisitNumberDataElementId: String? = null,
    @SerializedName("excludedDataElementIds")
    val excludedDataElementIds: List<String>? = null,
) {
    fun isValid(): Boolean = !programId.isNullOrBlank() && !programStageId.isNullOrBlank()
}
