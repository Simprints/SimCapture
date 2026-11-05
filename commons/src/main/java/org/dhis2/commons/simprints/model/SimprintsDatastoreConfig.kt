package org.dhis2.commons.simprints.model

import com.google.gson.annotations.SerializedName

data class SimprintsDatastoreConfig(
    val programSpecificSettings: List<ProgramSpecificSetting> = emptyList(),
)

data class ProgramSpecificSetting(
    @SerializedName("programId")
    val programId: String? = null,
    @SerializedName("isBiometricsCaptureOnlyButtonEnabledForAttributeId")
    val isBiometricsCaptureOnlyButtonEnabledForAttributeId: String? = null,
    @SerializedName("externalCredentialAttributeId")
    val externalCredentialAttributeId: String? = null,
) {
    fun isValid(): Boolean = !programId.isNullOrBlank()
}
