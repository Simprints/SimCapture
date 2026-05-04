package org.dhis2.form.ui.customintent

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.mobile.commons.model.CustomIntentRequestArgumentModel
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import timber.log.Timber
import kotlin.collections.forEach

class CustomIntentActivityResultContract : ActivityResultContract<CustomIntentInput, CustomIntentResult>() {
    private var pendingResultContext: CustomIntentResultContext? = null

    companion object {
        private val SIMPRINTS_IDENTIFICATION_EXTRA_NAMES = listOf("identification", "identifications")
        private const val SIMPRINTS_GUID_KEY = "guid"
    }

    override fun createIntent(
        context: Context,
        input: CustomIntentInput,
    ): Intent {
        val intentData =
            mapIntentData(input.customIntent.packageName, input.customIntent.customIntentRequest)
        pendingResultContext = CustomIntentResultContext.from(input)
        return Intent.createChooser(
            intentData,
            input.customIntent.name ?: input.defaultTitle,
        )
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): CustomIntentResult {
        val resultContext = pendingResultContext
        pendingResultContext = null

        return parseResult(
            resultCode = resultCode,
            intent = intent,
            resultContext = resultContext,
        )
    }

    fun parseResult(
        resultCode: Int,
        intent: Intent?,
        resultContext: CustomIntentResultContext?,
    ): CustomIntentResult {
        pendingResultContext = null
        if (resultContext == null) {
            Timber.e("Missing custom intent result context")
            return CustomIntentResult.Error(fieldUid = "")
        }

        return parseResultWithContext(
            resultCode = resultCode,
            intent = intent,
            resultContext = resultContext,
        )
    }

    private fun parseResultWithContext(
        resultCode: Int,
        intent: Intent?,
        resultContext: CustomIntentResultContext,
    ): CustomIntentResult =
        if (resultCode == RESULT_OK) {
            val customIntentResponseParsedData =
                mapIntentResponseData(resultContext.responseData, intent)
            if (customIntentResponseParsedData.isNullOrEmpty()) {
                mapPossibleSimprintsDuplicateResult(intent, resultContext)
                    ?: CustomIntentResult.Error(fieldUid = resultContext.fieldUid)
            } else {
                CustomIntentResult.Success(
                    fieldUid = resultContext.fieldUid,
                    value = customIntentResponseParsedData.joinToString(separator = ","),
                    action = resultContext.action,
                    extras = intent?.extras,
                )
            }
        } else {
            CustomIntentResult.Error(fieldUid = resultContext.fieldUid)
        }

    private fun mapPossibleSimprintsDuplicateResult(
        intent: Intent?,
        resultContext: CustomIntentResultContext,
    ): CustomIntentResult.PossibleDuplicates? {
        if (
            !resultContext.isSimprintsCallout ||
            resultContext.isSimprintsIdentifyCallout
        ) {
            return null
        }

        val guidValues =
            SIMPRINTS_IDENTIFICATION_EXTRA_NAMES.firstNotNullOfOrNull { extraName ->
                mapIntentResponseData(
                    customIntentResponse =
                        listOf(
                            CustomIntentResponseDataModel(
                                name = extraName,
                                extraType = CustomIntentResponseExtraType.LIST_OF_OBJECTS,
                                key = SIMPRINTS_GUID_KEY,
                            ),
                        ),
                    intent = intent,
                )
            } ?: return null

        return CustomIntentResult.PossibleDuplicates(
            fieldUid = resultContext.fieldUid,
            guidValues = guidValues,
            extras = intent?.extras,
        )
    }

    fun mapIntentData(
        packageName: String,
        requestParameters: List<CustomIntentRequestArgumentModel>,
    ): Intent =
        Intent(packageName).apply {
            requestParameters.forEach { argument ->
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

    fun mapIntentResponseData(
        customIntentResponse: List<CustomIntentResponseDataModel>?,
        intent: Intent?,
    ): List<String>? {
        val responseData = mutableListOf<String>()
        val objectCache = mapOf<String, JsonObject?>()
        val listCache = mapOf<String, List<JsonObject>?>()
        intent?.let { intent } ?: return null
        customIntentResponse?.forEach { extra ->
            if (!intent.hasExtra(extra.name)) return@forEach

            extractValue(extra, intent, objectCache, listCache)?.let {
                responseData.addAll(it)
            }
        }

        return responseData.ifEmpty { null }
    }

    private fun extractValue(
        extra: CustomIntentResponseDataModel,
        intent: Intent,
        objectCache: Map<String, JsonObject?>,
        listCache: Map<String, List<JsonObject>?>,
    ): List<String>? =
        when (extra.extraType) {
            CustomIntentResponseExtraType.STRING ->
                intent.getStringExtra(extra.name)?.let { listOf(it) }

            CustomIntentResponseExtraType.INTEGER ->
                listOf(intent.getIntExtra(extra.name, 0).toString())

            CustomIntentResponseExtraType.BOOLEAN ->
                listOf(intent.getBooleanExtra(extra.name, false).toString())

            CustomIntentResponseExtraType.FLOAT ->
                listOf(intent.getFloatExtra(extra.name, 0f).toString())

            CustomIntentResponseExtraType.OBJECT ->
                extractObjectValue(extra, intent, objectCache)

            CustomIntentResponseExtraType.LIST_OF_OBJECTS ->
                extractListValues(extra, intent, listCache)
        }

    private fun extractObjectValue(
        extra: CustomIntentResponseDataModel,
        intent: Intent,
        objectCache: Map<String, JsonObject?>,
    ): List<String>? {
        try {
            val jsonString = intent.getStringExtra(extra.name) ?: return null
            val jsonObject = objectCache[jsonString] ?: getComplexObject(jsonString) ?: return null

            return if (jsonObject.has(extra.key)) {
                listOf(jsonObject[extra.key].asString)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.d(e)
            return null
        }
    }

    private fun extractListValues(
        extra: CustomIntentResponseDataModel,
        intent: Intent,
        listCache: Map<String, List<JsonObject>?>,
    ): List<String>? {
        try {
            val jsonString = intent.getStringExtra(extra.name) ?: return null
            val objectsList = listCache[jsonString] ?: getListOfObjects(jsonString) ?: return null

            return objectsList
                .filter { it.has(extra.key) }
                .map { it[extra.key].asString }
                .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.d(e)
            return null
        }
    }

    fun getComplexObject(jsonString: String): JsonObject? =
        try {
            val gson = Gson()
            gson.fromJson(jsonString, JsonObject::class.java)
        } catch (e: Exception) {
            Timber.d(e)
            null
        }

    fun getListOfObjects(jsonString: String): List<JsonObject>? =
        try {
            val gson = Gson()
            val listType =
                TypeToken
                    .getParameterized(List::class.java, JsonObject::class.java)
                    .type
            gson.fromJson(jsonString, listType)
        } catch (e: Exception) {
            Timber.d(e)
            null
        }
}

data class CustomIntentInput(
    val fieldUid: String,
    val customIntent: CustomIntentModel,
    val defaultTitle: String,
)

data class CustomIntentResultContext(
    val fieldUid: String,
    val action: String,
    val responseData: List<CustomIntentResponseDataModel>,
    val isSimprintsCallout: Boolean,
    val isSimprintsIdentifyCallout: Boolean,
) {
    data class SavedState(
        val fieldUid: String,
        val action: String,
        val responseDataJson: String,
        val isSimprintsCallout: Boolean,
        val isSimprintsIdentifyCallout: Boolean,
    )

    fun toSavedState(): SavedState =
        SavedState(
            fieldUid = fieldUid,
            action = action,
            responseDataJson = Gson().toJson(responseData),
            isSimprintsCallout = isSimprintsCallout,
            isSimprintsIdentifyCallout = isSimprintsIdentifyCallout,
        )

    fun saveTo(
        outState: Bundle,
        keyPrefix: String,
    ) {
        with(toSavedState()){
            outState.putString("$keyPrefix.fieldUid", fieldUid)
            outState.putString("$keyPrefix.action", action)
            outState.putString("$keyPrefix.responseData", responseDataJson)
            outState.putBoolean("$keyPrefix.isSimprintsCallout", isSimprintsCallout)
            outState.putBoolean("$keyPrefix.isSimprintsIdentifyCallout", isSimprintsIdentifyCallout)
        }
    }

    companion object {
        fun from(input: CustomIntentInput): CustomIntentResultContext =
            CustomIntentResultContext(
                fieldUid = input.fieldUid,
                action = input.customIntent.packageName,
                responseData = input.customIntent.customIntentResponse,
                isSimprintsCallout = SimprintsIntentUtils.isCallout(input.customIntent),
                isSimprintsIdentifyCallout = SimprintsIntentUtils.isIdentifyCallout(input.customIntent),
            )

        fun restoreFrom(
            savedState: Bundle?,
            keyPrefix: String,
        ): CustomIntentResultContext? {
            val fieldUid =
                savedState?.getString("$keyPrefix.fieldUid")?.takeIf { it.isNotBlank() }
                    ?: return null
            val action =
                savedState.getString("$keyPrefix.action")?.takeIf { it.isNotBlank() }
                    ?: return null
            val responseDataJson =
                savedState.getString("$keyPrefix.responseData")
                    ?: return null

            return restoreFrom(
                SavedState(
                    fieldUid = fieldUid,
                    action = action,
                    responseDataJson = responseDataJson,
                    isSimprintsCallout = savedState.getBoolean("$keyPrefix.isSimprintsCallout"),
                    isSimprintsIdentifyCallout =
                        savedState.getBoolean("$keyPrefix.isSimprintsIdentifyCallout"),
                ),
            )
        }

        fun restoreFrom(savedState: SavedState?): CustomIntentResultContext? {
            val fieldUid =
                savedState?.fieldUid?.takeIf { it.isNotBlank() }
                    ?: return null
            val action =
                savedState.action.takeIf { it.isNotBlank() }
                    ?: return null
            val responseData =
                parseResponseData(savedState.responseDataJson)
                    ?: return null

            return CustomIntentResultContext(
                fieldUid = fieldUid,
                action = action,
                responseData = responseData,
                isSimprintsCallout = savedState.isSimprintsCallout,
                isSimprintsIdentifyCallout = savedState.isSimprintsIdentifyCallout,
            )
        }

        private fun parseResponseData(json: String): List<CustomIntentResponseDataModel>? =
            try {
                Gson().fromJson<List<CustomIntentResponseDataModel>>(
                    json,
                    object : TypeToken<List<CustomIntentResponseDataModel>>() {}.type,
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore custom intent response data")
                null
            }
    }
}

sealed class CustomIntentResult {
    data class Success(
        val fieldUid: String,
        val value: String,
        val action: String?,
        val extras: Bundle?,
    ) : CustomIntentResult()

    data class Error(
        val fieldUid: String,
    ) : CustomIntentResult()

    data class PossibleDuplicates(
        val fieldUid: String,
        val guidValues: List<String>,
        val extras: Bundle?,
    ) : CustomIntentResult()
}
