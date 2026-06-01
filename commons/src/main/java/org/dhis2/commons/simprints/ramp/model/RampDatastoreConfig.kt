package org.dhis2.commons.simprints.ramp.model

import com.google.gson.annotations.SerializedName

data class RampDatastoreConfig(
    val dataElementHistoryCharts: List<DataElementHistoryChartConfig> = emptyList(),
) {
    fun isNotEmpty(): Boolean = dataElementHistoryCharts.isNotEmpty()
}

data class DataElementHistoryChartConfig(
    @SerializedName("programId")
    val programId: String? = null,
    @SerializedName("followUpVisitProgramStageId")
    val followUpVisitProgramStageId: String? = null,
    @SerializedName("dataElementId")
    val dataElementId: String? = null,
    @SerializedName("xAxisVisitNumberDataElementId")
    val xAxisVisitNumberDataElementId: String? = null,
    @SerializedName("followUpVisitMaxNumber")
    val followUpVisitMaxNumber: Int? = null,
) {
    fun isValid(): Boolean =
        !programId.isNullOrBlank() &&
            !followUpVisitProgramStageId.isNullOrBlank() &&
            !dataElementId.isNullOrBlank() &&
            !xAxisVisitNumberDataElementId.isNullOrBlank() &&
            (followUpVisitMaxNumber ?: -1) >= 0
}
