package org.dhis2.usescases.searchTrackEntity.searchparameters

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanOptions
import org.dhis2.commons.Constants
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.form.R
import org.dhis2.form.data.scan.ScanContract
import org.dhis2.form.ui.customintent.CustomIntentActivityResultContract
import org.dhis2.form.ui.customintent.CustomIntentInput
import org.dhis2.form.ui.customintent.CustomIntentResult
import org.dhis2.mobile.commons.extensions.ObserveAsEvents
import org.dhis2.mobile.commons.orgunit.OrgUnitSelectorScope
import org.dhis2.tracker.input.ui.action.TrackerInputAction
import org.dhis2.tracker.input.ui.action.TrackerInputUiEvent
import org.dhis2.tracker.search.ui.action.SearchScreenUiEvent
import org.dhis2.tracker.search.ui.screen.SearchParametersScreen
import org.dhis2.usescases.searchTrackEntity.SearchTEIViewModel
import timber.log.Timber

fun provideSearchScreen(
    composeView: ComposeView,
    viewModel: SearchTEIViewModel,
    program: String?,
    teiType: String,
    resources: ResourceManager,
    onShowOrgUnit: (
        uid: String,
        preselectedOrgUnits: List<String>,
        orgUnitScope: OrgUnitSelectorScope,
        label: String,
    ) -> Unit,
    onClear: () -> Unit,
) {
    viewModel.fetchSearchParameters(
        programUid = program,
        teiTypeUid = teiType,
    )
    composeView.setContent {
        var pendingSimprintsFieldUid by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingSimprintsResponseDataJson by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingSimprintsCapturesSessionId by rememberSaveable { mutableStateOf(false) }
        val customIntentActivityResultContract = remember { CustomIntentActivityResultContract() }
        val customIntentLauncher =
            rememberLauncherForActivityResult(
                contract = customIntentActivityResultContract,
                onResult = viewModel::handleCustomIntentResult,
            )
        val simprintsIdentifyLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val uid = pendingSimprintsFieldUid
                val responseDataJson = pendingSimprintsResponseDataJson
                val capturesSessionId = pendingSimprintsCapturesSessionId

                pendingSimprintsFieldUid = null
                pendingSimprintsResponseDataJson = null
                pendingSimprintsCapturesSessionId = false

                if (uid != null) {
                    viewModel.onSimprintsBiometricSearchResult(
                        uid = uid,
                        responseDataJson = responseDataJson,
                        resultCode = result.resultCode,
                        data = result.data,
                        capturesSessionId = capturesSessionId,
                    )
                } else {
                    viewModel.onSimprintsBiometricSearchDropout()
                }
            }

        val scanContract = remember { ScanContract() }

        val qrScanLauncher =
            rememberLauncherForActivityResult(
                contract = scanContract,
            ) { result ->
                result.contents?.let { qrData ->
                    viewModel.handleScanResult(
                        fieldUid = result.originalIntent.getStringExtra(Constants.UID)!!,
                        value = qrData,
                    )
                }
            }

        ObserveAsEvents(
            flow = viewModel.searchActions,
        ) { action ->
            when (action) {
                is TrackerInputAction.LaunchCustomIntent -> {
                    if (SimprintsIntentUtils.isIdentifyCallout(action.customIntentModel)) {
                        val preparedCallout = SimprintsIntentUtils.prepareCallout(action.customIntentModel)
                        pendingSimprintsFieldUid = action.fieldUid
                        pendingSimprintsResponseDataJson = Gson().toJson(preparedCallout.responseData)
                        pendingSimprintsCapturesSessionId = true
                        try {
                            simprintsIdentifyLauncher.launch(preparedCallout.launchIntent)
                        } catch (e: Exception) {
                            Timber.e(e)
                            pendingSimprintsFieldUid = null
                            pendingSimprintsResponseDataJson = null
                            pendingSimprintsCapturesSessionId = false
                            viewModel.handleCustomIntentResult(
                                CustomIntentResult.Error(fieldUid = action.fieldUid),
                            )
                        }
                        return@ObserveAsEvents
                    }

                    customIntentLauncher.launch(
                        with(action) {
                            CustomIntentInput(
                                fieldUid = fieldUid,
                                customIntent = customIntentModel,
                                defaultTitle =
                                    customIntentModel.name
                                        ?: resources.getString(R.string.select_app_intent),
                            )
                        },
                    )
                }

                is TrackerInputAction.Scan -> {
                    with(action) {
                        qrScanLauncher.launch(
                            ScanOptions().apply {
                                setDesiredBarcodeFormats()
                                setPrompt("")
                                setBeepEnabled(true)
                                setBarcodeImageEnabled(false)
                                addExtra(Constants.UID, fieldUid)
                                optionSet?.let {
                                    addExtra(
                                        Constants.OPTION_SET,
                                        it,
                                    )
                                }
                                addExtra(
                                    Constants.SCAN_RENDERING_TYPE,
                                    renderType,
                                )
                            },
                        )
                    }
                }

                is TrackerInputAction.ValueChanged -> {
                    viewModel.onValueChange(
                        fieldUid = action.fieldUid,
                        value = action.value,
                    )
                }
            }
        }

        val configuration = LocalConfiguration.current

        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        SearchParametersScreen(
            externalUiState = viewModel.searchParametersUiState,
            onSearchScreenUiEvent = {
                when (it) {
                    is SearchScreenUiEvent.OnSearchButtonClicked ->
                        viewModel.onSearch()

                    is SearchScreenUiEvent.OnClearSearchButtonClicked -> {
                        onClear()
                        viewModel.clearQueryData()
                        viewModel.clearFocus()
                    }

                    is SearchScreenUiEvent.OnCloseClicked -> viewModel.clearFocus()
                }
            },
            isLandscape = isLandscape,
            onTrackerInputUiEvent = {
                when (it) {
                    is TrackerInputUiEvent.OnScanButtonClicked ->
                        viewModel.launchScan(
                            it.uid,
                            it.optionSet,
                            it.renderType,
                        )

                    is TrackerInputUiEvent.OnOrgUnitButtonClicked ->
                        onShowOrgUnit(
                            it.uid,
                            it.value?.let { listOf(it) }
                                ?: emptyList(),
                            it.orgUnitSelectorScope
                                ?: OrgUnitSelectorScope.UserSearchScope(),
                            it.label,
                        )

                    is TrackerInputUiEvent.OnLaunchCustomIntent ->
                        viewModel.launchCustomIntent(
                            it.uid,
                            it.customIntentUid,
                        )

                    is TrackerInputUiEvent.OnItemClick -> viewModel.onItemClick(it.uid)

                    is TrackerInputUiEvent.OnValueChange ->
                        viewModel.onValueChange(
                            fieldUid = it.uid,
                            value = it.value,
                        )
                }
            },
            getOptionSetFlow = { fieldUid, optionSetUid ->
                viewModel.getOptionSetFlow(fieldUid, optionSetUid)
            },
            onOptionSetSearch = { fieldUid, query ->
                viewModel.onOptionSetSearch(fieldUid, query)
            },
        )
    }
}
