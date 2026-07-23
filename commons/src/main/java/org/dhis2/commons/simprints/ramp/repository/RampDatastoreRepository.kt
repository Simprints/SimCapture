package org.dhis2.commons.simprints.ramp.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import org.dhis2.commons.simprints.ramp.model.DataElementHistoryChartConfig
import org.dhis2.commons.simprints.ramp.model.ProgramSpecificSetting
import org.dhis2.commons.simprints.ramp.model.ProgramStageHistoryTableConfig
import org.dhis2.commons.simprints.ramp.model.ProgramStageSpecificSetting
import org.dhis2.commons.simprints.ramp.model.RampDatastoreConfig
import org.hisp.dhis.android.core.D2
import timber.log.Timber

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

    fun isSearchEnabled(programId: String?): Boolean {
        val normalizedProgramId = programId.trimToValue() ?: return true

        return try {
            getConfig()
                .programSpecificSettings
                .firstOrNull { it.programId == normalizedProgramId }
                ?.isSearchEnabled != false
        } catch (exception: RuntimeException) {
            Timber.e(exception, RAMP_DATASTORE_PARSE_ERROR)
            true
        }
    }

    fun isScheduleOptionEnabled(programStageId: String?): Boolean = isProgramStageOptionEnabled(programStageId) { isScheduleOptionEnabled }

    fun isReferOptionEnabled(programStageId: String?): Boolean = isProgramStageOptionEnabled(programStageId) { isReferOptionEnabled }

    private fun isProgramStageOptionEnabled(
        programStageId: String?,
        option: ProgramStageSpecificSetting.() -> Boolean?,
    ): Boolean {
        val normalizedProgramStageId = programStageId.trimToValue() ?: return true

        return try {
            getConfig()
                .programStageSpecificSettings
                .firstOrNull { it.programStageId == normalizedProgramStageId }
                ?.option() != false
        } catch (exception: RuntimeException) {
            Timber.e(exception, RAMP_DATASTORE_PARSE_ERROR)
            true
        }
    }

    private fun getLocalRampDatastoreValue(): String? =
        d2
            .dataStoreModule()
            .dataStore()
            .value(RAMP_DATASTORE_NAMESPACE, RAMP_DATASTORE_KEY)
            .blockingGet()
            ?.value()

    private fun parseRawValue(value: String): RampDatastoreConfig =
        try {
            val root = parseDatastoreJsonElement(value).asJsonObject

            RampDatastoreConfig(
                dataElementHistoryCharts =
                    root
                        .get(DATA_ELEMENT_HISTORY_CHARTS_KEY)
                        ?.parseList<DataElementHistoryChartConfig>()
                        .orEmpty()
                        .map { it.normalized() }
                        .filter { it.isValid() },
                programStageHistoryTables =
                    root
                        .get(PROGRAM_STAGE_HISTORY_TABLE_KEY)
                        ?.parseList<ProgramStageHistoryTableConfig>()
                        .orEmpty()
                        .map { it.normalized() }
                        .filter { it.isValid() },
                programSpecificSettings =
                    root
                        .get(PROGRAM_SPECIFIC_SETTINGS_KEY)
                        ?.parseList<ProgramSpecificSetting>()
                        .orEmpty()
                        .map { it.normalized() }
                        .filter { it.isValid() },
                programStageSpecificSettings =
                    root
                        .get(PROGRAM_STAGE_SPECIFIC_SETTINGS_KEY)
                        ?.parseList<ProgramStageSpecificSetting>()
                        .orEmpty()
                        .map { it.normalized() }
                        .filter { it.isValid() },
            )
        } catch (exception: JsonParseException) {
            Timber.e(exception, RAMP_DATASTORE_PARSE_ERROR)
            RampDatastoreConfig()
        } catch (exception: IllegalStateException) {
            Timber.e(exception, RAMP_DATASTORE_PARSE_ERROR)
            RampDatastoreConfig()
        }

    private fun JsonElement.parseDatastoreJsonElement(): JsonElement =
        if (isJsonPrimitive && asJsonPrimitive.isString) {
            parseDatastoreJsonElement(asString)
        } else {
            this
        }

    private fun parseDatastoreJsonElement(value: String): JsonElement =
        parseJsonWrapperElement(value) ?: parseJsonElement(value)

    private fun parseJsonElement(value: String): JsonElement =
        JsonParser.parseString(value).parseDatastoreJsonElement()

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
            Timber.e(exception, RAMP_DATASTORE_PARSE_ERROR)
            null
        } catch (exception: IllegalStateException) {
            Timber.e(exception, RAMP_DATASTORE_PARSE_ERROR)
            null
        }

    private fun DataElementHistoryChartConfig.normalized(): DataElementHistoryChartConfig =
        copy(
            programId = programId.trimToValue(),
            followUpVisitProgramStageId = followUpVisitProgramStageId.trimToValue(),
            dataElementId = dataElementId.trimToValue(),
            xAxisVisitNumberDataElementId = xAxisVisitNumberDataElementId.trimToValue(),
        )

    private fun ProgramStageHistoryTableConfig.normalized(): ProgramStageHistoryTableConfig =
        copy(
            programId = programId.trimToValue(),
            followUpVisitProgramStageId = followUpVisitProgramStageId.trimToValue(),
            headerVisitNumberDataElementId = headerVisitNumberDataElementId.trimToValue(),
            excludedFollowUpVisitDataElementIds =
                excludedFollowUpVisitDataElementIds
                    ?.mapNotNull { it.trimToValue() },
        )

    private fun ProgramSpecificSetting.normalized(): ProgramSpecificSetting =
        copy(programId = programId.trimToValue())

    private fun ProgramStageSpecificSetting.normalized(): ProgramStageSpecificSetting =
        copy(
            programStageId = programStageId.trimToValue(),
            visitNumberDataElementId = visitNumberDataElementId.trimToValue(),
        )

    private fun String?.trimToValue(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private data class CachedConfig(
        val rawValue: String?,
        val config: RampDatastoreConfig,
    )

    private companion object {
        private const val RAMP_DATASTORE_NAMESPACE = "simprints"
        private const val RAMP_DATASTORE_KEY = "ramp"
        private const val DATA_ELEMENT_HISTORY_CHARTS_KEY = "dataElementHistoryCharts"
        private const val PROGRAM_STAGE_HISTORY_TABLE_KEY = "programStageHistoryTable"
        private const val PROGRAM_SPECIFIC_SETTINGS_KEY = "programSpecificSettings"
        private const val PROGRAM_STAGE_SPECIFIC_SETTINGS_KEY = "programStageSpecificSettings"
        private const val DATASTORE_JSON_WRAPPER_NAME = "JsonWrapper"
        private const val DATASTORE_JSON_WRAPPER_JSON_FIELD = "json"
        private const val RAMP_DATASTORE_PARSE_ERROR = "Failed to parse Simprints RAMP datastore config"
    }
}
