package com.simprints.simprints

// State of biometrics of a Simprints beneficiary / DHIS2 TEI. Properties availability: best effort.
data class SimprintsBiometricsState(
    val teiUid: String? = null,
    val simprintsGuid: String? = null,
    val programUid: String? = null,
    val enrollmentUid: String? = null, // DHIS2-specific enrollment of TEI in Program; not biometric
    val simprintsProjectId: String? = null,
    val orgUnitUid: String? = null,
    val simprintsModuleId: String? = null,
    val userId: String? = null,
    val simprintsMatchThreshold: Int? = null,
    val biometricLockingTimeoutMinutes: Int? = null,
    val lastBiometricsResultTimestamp: Long? = null,
    val lastBiometricsResultSuccess: Boolean? = null,
) {

    // A biometrically locked TEI conceptually requires unlocking by the means of
    // biometric verification or identification of the beneficiary by the Simprints ID app.
    // Biometric enrollment unlocks the newly enrolled beneficiary right away.
    fun isLocked(millisNow: Long): Boolean = when {
        simprintsGuid.isNullOrBlank() -> true
        biometricLockingTimeoutMinutes == null -> true
        biometricLockingTimeoutMinutes < 0 -> true
        lastBiometricsResultSuccess != true -> true
        lastBiometricsResultTimestamp == null -> true
        millisNow > lastBiometricsResultTimestamp + biometricLockingTimeoutMinutes * 60_000 -> true
        else -> false
    }
}
