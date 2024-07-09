package com.simprints.simprints.ui

import androidx.databinding.ObservableField
import com.simprints.simprints.SimprintsBiometricsAction
import com.simprints.simprints.SimprintsBiometricsState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

// Reactively updating Simprints biometrics readout UI on state's flow updates, by using ViewBinding
// (this approach taken for simpler compatibility with other DHIS2 UI)
data class SimprintsBiometricsUiModel(
    val teiStateFlow: StateFlow<SimprintsBiometricsState>,
    val onInteraction: (SimprintsBiometricsAction) -> Unit,
) {

    fun onClick() {
        onInteraction(SimprintsBiometricsAction(isOneToMany = false))
    }

    val teiUid = ObservableField<String>()
    val simprintsGuid = ObservableField<String>()
    val simprintsSubjectActions = ObservableField<String>()
    val programUid = ObservableField<String>()
    val simprintsProjectId = ObservableField<String>()
    val orgUnitUid = ObservableField<String>()
    val simprintsModuleId = ObservableField<String>()
    val userId = ObservableField<String>()
    val simprintsBiometricActionAvailability = ObservableField<Boolean>()
    val simprintsBiometricActionName = ObservableField<String>()
    val simprintsMatchThreshold = ObservableField<String>()
    val biometricLockingTimeout = ObservableField<String>()
    val lastBiometricsTime = ObservableField<String>()
    val lastBiometricsSuccess = ObservableField<String>()

    init {
        GlobalScope.launch { // todo provide coroutineScope
            teiStateFlow.collect {
                it.let {
                    teiUid.setOrNone(it.teiUid)
                    simprintsGuid.setOrNone(it.simprintsGuid)
                    simprintsSubjectActions.setOrNone(it.simprintsSubjectActions?.take(100)) // truncated data view
                    programUid.setOrNone(it.programUid)
                    simprintsProjectId.setOrNone(it.simprintsProjectId)
                    orgUnitUid.setOrNone(it.orgUnitUid)
                    simprintsModuleId.setOrNone(it.simprintsModuleId)
                    userId.setOrNone(it.userId)
                    simprintsBiometricActionAvailability.set(it.isBiometricsActionAvailable())
                    simprintsBiometricActionName.setOrNone(it.getBiometricsActionName())
                    simprintsMatchThreshold.setOrNone(it.simprintsMatchThreshold?.toString())
                    biometricLockingTimeout.setOrNone(it.biometricLockingTimeoutMinutes?.toString())
                    lastBiometricsTime.setOrNone(it.lastBiometricsResultTime())
                    lastBiometricsSuccess.setOrNone(it.lastBiometricsResultSuccess?.toString())
                }
            }
        }
    }

    private fun ObservableField<String>.setOrNone(value: String?) {
        set(value.takeIf { !it.isNullOrBlank() } ?: "<none>")
    }

    private fun SimprintsBiometricsState.isBiometricsActionAvailable(): Boolean =
        when {
            simprintsProjectId.isNullOrBlank() -> false
            simprintsModuleId.isNullOrBlank() -> false
            userId.isNullOrBlank() -> false
            simprintsMatchThreshold == null -> false
            else -> true
        }

    private fun SimprintsBiometricsState.getBiometricsActionName(): String =
        when {
            simprintsProjectId.isNullOrBlank() -> "Biometrics unavailable" // todo extract to XML
            simprintsModuleId.isNullOrBlank() -> "Biometrics unavailable"
            userId.isNullOrBlank() -> "Biometrics unavailable"
            simprintsGuid.isNullOrBlank() -> "Enroll"
            else -> "Verify"
        }

    private fun SimprintsBiometricsState.lastBiometricsResultTime(): String? =
        lastBiometricsResultTimestamp.takeIf { it != null }?.run {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(this)
        }
}
