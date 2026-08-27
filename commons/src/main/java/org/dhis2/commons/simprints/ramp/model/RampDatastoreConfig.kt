package org.dhis2.commons.simprints.ramp.model

import com.google.gson.annotations.SerializedName

data class RampDatastoreConfig(
    val dataElementHistoryCharts: List<DataElementHistoryChartConfig> = emptyList(),
    val programStageHistoryTables: List<ProgramStageHistoryTableConfig> = emptyList(),
    val programSpecificSettings: List<ProgramSpecificSetting> = emptyList(),
    val programStageSpecificSettings: List<ProgramStageSpecificSetting> = emptyList(),
) {
    fun isNotEmpty(): Boolean =
        dataElementHistoryCharts.isNotEmpty() ||
            programStageHistoryTables.isNotEmpty() ||
            programSpecificSettings.isNotEmpty() ||
            programStageSpecificSettings.isNotEmpty()
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
    @SerializedName("displayMaxDecimalPlaces")
    val displayMaxDecimalPlaces: Int? = null,
    @SerializedName("isYAxisInverted")
    val isYAxisInverted: Boolean = false,
) {
    fun isValid(): Boolean =
        !programId.isNullOrBlank() &&
            !followUpVisitProgramStageId.isNullOrBlank() &&
            !dataElementId.isNullOrBlank() &&
            !xAxisVisitNumberDataElementId.isNullOrBlank() &&
            (followUpVisitMaxNumber ?: -1) >= 0
}

data class ProgramStageHistoryTableConfig(
    @SerializedName("programId")
    val programId: String? = null,
    @SerializedName("followUpVisitProgramStageId")
    val followUpVisitProgramStageId: String? = null,
    @SerializedName("followUpVisitMaxNumber")
    val followUpVisitMaxNumber: Int? = null,
    @SerializedName("headerVisitNumberDataElementId")
    val headerVisitNumberDataElementId: String? = null,
    @SerializedName("excludedFollowUpVisitDataElementIds")
    val excludedFollowUpVisitDataElementIds: List<String>? = null,
) {
    fun isValid(): Boolean =
        !programId.isNullOrBlank() &&
            !followUpVisitProgramStageId.isNullOrBlank() &&
            !headerVisitNumberDataElementId.isNullOrBlank() &&
            (followUpVisitMaxNumber ?: -1) >= 0
}

data class ProgramSpecificSetting(
    @SerializedName("programId")
    val programId: String? = null,
    @SerializedName("isOneLevelUpOrgUnitForBiometricsModuleId")
    val isOneLevelUpOrgUnitForBiometricsModuleId: Boolean? = null,
    @SerializedName("isSearchEnabled")
    val isSearchEnabled: Boolean? = null,
    @SerializedName("isShowingUnfilteredList")
    val isShowingUnfilteredList: Boolean? = null,
    @SerializedName("hasDetailedEnrollmentListing")
    val hasDetailedEnrollmentListing: Boolean? = null,
    @SerializedName("detailedEnrollmentListingDischargeOutcomeDataElementIds")
    val detailedEnrollmentListingDischargeOutcomeDataElementIds: List<String>? = null,
    @SerializedName("detailedEnrollmentListingAdmissionProgramStageIds")
    val detailedEnrollmentListingAdmissionProgramStageIds: List<String>? = null,
    @SerializedName("detailedEnrollmentListingDischargeProgramStageIds")
    val detailedEnrollmentListingDischargeProgramStageIds: List<String>? = null,
) {
    fun isValid(): Boolean = !programId.isNullOrBlank()
}

data class DetailedEnrollmentListingSettings(
    val dischargeOutcomeDataElementIds: Set<String>,
    val admissionProgramStageIds: Set<String>,
    val dischargeProgramStageIds: Set<String>,
)

data class ProgramStageSpecificSetting(
    @SerializedName("programStageId")
    val programStageId: String? = null,
    @SerializedName("hasVisitNumberPrefixForDateInList")
    val hasVisitNumberPrefixForDateInList: Boolean? = null,
    @SerializedName("visitNumberDataElementId")
    val visitNumberDataElementId: String? = null,
    @SerializedName("isScheduleOptionEnabled")
    val isScheduleOptionEnabled: Boolean? = null,
    @SerializedName("isReferOptionEnabled")
    val isReferOptionEnabled: Boolean? = null,
    @SerializedName("isScheduleAnchoredToInitialVisit")
    val isScheduleAnchoredToInitialVisit: Boolean = false,
) {
    fun isValid(): Boolean = !programStageId.isNullOrBlank()
}
