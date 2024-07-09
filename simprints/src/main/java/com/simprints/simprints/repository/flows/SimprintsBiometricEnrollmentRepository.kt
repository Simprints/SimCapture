package com.simprints.simprints.repository.flows

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.simprints.libsimprints.Constants
import com.simprints.libsimprints.Registration
import com.simprints.libsimprints.SimHelper
import com.simprints.simprints.repository.flows.SimprintsBiometricConstants.DATA_SOURCE_COMMCARE
import com.simprints.simprints.repository.flows.SimprintsBiometricConstants.MODULE_ID
import com.simprints.simprints.repository.flows.SimprintsBiometricConstants.PROJECT_ID
import com.simprints.simprints.repository.flows.SimprintsBiometricConstants.USER_ID
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

data class SimprintsBiometricEnrollmentResult(
    val simprintsGuid: String? = null,
    val simprintsSubjectActions: String? = null,
    val biometricsResultSuccess: Boolean? = null,
)

class SimprintsBiometricEnrollmentRepository @Inject constructor(
    private val context: Context,
) {

    private companion object {
        // This SimCapture app, after calling Simprints ID app, may be removed from memory by system
        // so the Simprints ID result is returned by a flow rather than as a function return value.
        private val enrollmentResultFlow: MutableSharedFlow<SimprintsBiometricEnrollmentResult> =
            MutableSharedFlow(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        private fun Intent?.get(key: String): String =
            this?.getStringExtra(key) ?: ""
    }

    fun getEnrollmentResultFlow(): SharedFlow<SimprintsBiometricEnrollmentResult> =
        enrollmentResultFlow

    fun launchEnroll(projectId: String, moduleId: String, userId: String) {
        context.startActivity(
            Intent(context, SimprintsBiometricEnrollmentHandlingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(PROJECT_ID, projectId)
                putExtra(MODULE_ID, moduleId)
                putExtra(USER_ID, userId)
                putExtra(Constants.SIMPRINTS_BIOMETRIC_DATA_SOURCE, DATA_SOURCE_COMMCARE)
            },
        )
    }

    // Serves as an Android-specific and Simprints ID-compatible data transporter
    // between this SimCapture and the separate Simprints ID app. Not part of the UI.
    class SimprintsBiometricEnrollmentHandlingActivity : ComponentActivity() {

        private val registerForEnrollResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { activityResult ->
            activityResult.data?.takeIf { intent ->
                intent.getBooleanExtra(Constants.SIMPRINTS_BIOMETRICS_COMPLETE_CHECK, false)
            }?.run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableExtra(Constants.SIMPRINTS_REGISTRATION, Registration::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    getParcelableExtra(Constants.SIMPRINTS_REGISTRATION)
                }
            }?.guid.let { guid ->
                enrollmentResultFlow.tryEmit(
                    SimprintsBiometricEnrollmentResult(
                        simprintsGuid = guid,
                        simprintsSubjectActions = activityResult.data
                            ?.getStringExtra(Constants.SIMPRINTS_COSYNC_SUBJECT_ACTIONS),
                        biometricsResultSuccess = guid != null,
                    ),
                )
            }
            finish()
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            registerForEnrollResult.launch(
                with(intent) {
                    SimHelper(get(PROJECT_ID), get(USER_ID)).register(get(MODULE_ID))
                },
            )
        }
    }
}
