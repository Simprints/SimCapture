package org.dhis2.simprints.ramp.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dhis2.bindings.app
import org.dhis2.commons.Constants
import org.dhis2.simprints.ramp.di.EventHistoryTableModule
import org.dhis2.usescases.eventsWithoutRegistration.eventCapture.EventCaptureActivity
import org.dhis2.usescases.general.FragmentGlobalAbstract
import org.dhis2.usescases.teiDashboard.TeiDashboardMobileActivity
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import javax.inject.Inject

class EventHistoryTableFragment : FragmentGlobalAbstract() {
    @Inject
    lateinit var viewModelFactory: EventHistoryTableViewModelFactory

    private val viewModel: EventHistoryTableViewModel by viewModels { viewModelFactory }
    private var hasResumed = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        when (context) {
            is EventCaptureActivity ->
                context.eventCaptureComponent
                    ?.plus(
                        EventHistoryTableModule(
                            eventUid = requireArguments().getString(Constants.EVENT_UID).orEmpty(),
                        ),
                    )?.inject(this)

            is TeiDashboardMobileActivity ->
                context
                    .app()
                    .dashboardComponent()
                    ?.plus(
                        EventHistoryTableModule(
                            programUid = requireArguments().getString(Constants.PROGRAM_UID).orEmpty(),
                            enrollmentUid = requireArguments().getString(Constants.ENROLLMENT_UID).orEmpty(),
                        ),
                    )?.inject(this)

            else -> error("EventHistoryTableFragment must be attached to a supported activity")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DHIS2Theme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    EventHistoryTableScreen(
                        modifier = Modifier,
                        state = uiState,
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        if (hasResumed) {
            viewModel.load()
        } else {
            hasResumed = true
        }
    }

    companion object {
        fun newInstance(eventUid: String): EventHistoryTableFragment =
            EventHistoryTableFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(Constants.EVENT_UID, eventUid)
                    }
            }

        fun newEnrollmentInstance(
            programUid: String,
            enrollmentUid: String,
        ): EventHistoryTableFragment =
            EventHistoryTableFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(Constants.PROGRAM_UID, programUid)
                        putString(Constants.ENROLLMENT_UID, enrollmentUid)
                    }
            }
    }
}
