package org.dhis2.commons.simprints.repository.flows

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.simprints.libsimprints.Constants
import com.simprints.libsimprints.Identification
import com.simprints.libsimprints.SimHelper
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.dhis2.commons.simprints.repository.flows.SimprintsBiometricConstants.MODULE_ID
import org.dhis2.commons.simprints.repository.flows.SimprintsBiometricConstants.PROJECT_ID
import org.dhis2.commons.simprints.repository.flows.SimprintsBiometricConstants.USER_ID
import javax.inject.Inject

data class SimprintsBiometricIdentificationResult(
    val simprintsGuid: String,
    val simprintsMatchConfidenceScore: Int,
)

class SimprintsBiometricIdentificationRepository @Inject constructor(
    private val context: Context,
) {

    private companion object {
        // This SimCapture app, after calling Simprints ID app, may be removed from memory by system
        // so the Simprints ID result is returned by a flow rather than as a function return value.
        private val identificationResultFlow: MutableSharedFlow<List<SimprintsBiometricIdentificationResult>> =
            MutableSharedFlow(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        private fun Intent?.get(key: String): String =
            this?.getStringExtra(key) ?: ""
    }

    fun getIdentificationResultFlow(): SharedFlow<List<SimprintsBiometricIdentificationResult>> =
        identificationResultFlow

    fun launchIdentify(projectId: String, moduleId: String, userId: String) {
        context.startActivity(
            Intent(context, SimprintsBiometricIdentificationHandlingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(PROJECT_ID, projectId)
                putExtra(MODULE_ID, moduleId)
                putExtra(USER_ID, userId)
            },
        )
    }

    // Serves as an Android-specific and Simprints ID-compatible data transporter
    // between this SimCapture and the separate Simprints ID app. Not part of the UI.
    class SimprintsBiometricIdentificationHandlingActivity : ComponentActivity() {

        private val registerForIdentifyResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { activityResult ->
            activityResult.data?.takeIf { intent ->
                intent.getBooleanExtra(Constants.SIMPRINTS_BIOMETRICS_COMPLETE_CHECK, false)
            }?.run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableArrayListExtra(
                        Constants.SIMPRINTS_IDENTIFICATIONS,
                        Identification::class.java,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    getParcelableArrayListExtra(Constants.SIMPRINTS_IDENTIFICATIONS)
                }
            }?.mapNotNull { identification ->
                identification?.takeIf { it.guid != null }?.let {
                    SimprintsBiometricIdentificationResult(
                        simprintsGuid = identification.guid,
                        simprintsMatchConfidenceScore = identification.confidence.toInt(),
                    )
                }
            }.let { results ->
                identificationResultFlow.tryEmit(results ?: emptyList())
            }
            finish()
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            registerForIdentifyResult.launch(
                with(intent) {
                    SimHelper(get(PROJECT_ID), get(USER_ID)).identify(get(MODULE_ID))
                },
            )
        }
    }
}
