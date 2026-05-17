package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.history

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhis2.commons.Constants
import org.dhis2.usescases.eventsWithoutRegistration.eventCapture.EventCaptureActivity
import org.dhis2.usescases.general.FragmentGlobalAbstract
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import javax.inject.Inject

class EventHistoryTableFragment : FragmentGlobalAbstract() {
    @Inject
    lateinit var repository: EventHistoryTableRepository

    private var refreshKey by mutableIntStateOf(0)
    private var hasLoaded = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = context as EventCaptureActivity
        activity.eventCaptureComponent
            ?.plus(
                EventHistoryTableModule(
                    eventUid = requireArguments().getString(Constants.EVENT_UID).orEmpty(),
                ),
            )?.inject(this)
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
                    var uiState by remember {
                        mutableStateOf<EventHistoryTableUiState>(EventHistoryTableUiState.Loading)
                    }

                    LaunchedEffect(refreshKey) {
                        if (uiState !is EventHistoryTableUiState.Success) {
                            uiState = EventHistoryTableUiState.Loading
                        }
                        uiState =
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    repository
                                        .table()
                                        ?.let(EventHistoryTableUiState::Success)
                                        ?: EventHistoryTableUiState.Empty
                                }.getOrElse { error ->
                                    EventHistoryTableUiState.Error(error.message)
                                }
                            }
                    }

                    EventHistoryTableScreen(
                        modifier = Modifier,
                        state = uiState,
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        if (hasLoaded) {
            refreshKey += 1
        } else {
            hasLoaded = true
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
    }
}
