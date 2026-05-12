package org.dhis2.usescases.searchTrackEntity.searchparameters

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.dhis2.R
import org.dhis2.commons.Constants
import org.dhis2.commons.resources.ColorUtils
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.simprints.usecases.SimprintsHasAutoOpenEligibleIdentificationUseCase
import org.dhis2.commons.simprints.utils.SimprintsIntentUtils
import org.dhis2.form.data.scan.ScanContract
import org.dhis2.form.di.Injector
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.model.FieldUiModelImpl
import org.dhis2.form.ui.customintent.CustomIntentActivityResultContract
import org.dhis2.form.ui.event.RecyclerViewUiEvents
import org.dhis2.form.ui.intent.FormIntent
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.orgunit.OrgUnitSelectorScope
import org.dhis2.usescases.searchTrackEntity.SearchTEIViewModel
import org.dhis2.usescases.searchTrackEntity.searchparameters.model.SearchParametersUiState
import org.dhis2.usescases.searchTrackEntity.searchparameters.provider.provideParameterSelectorItem
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.mobile.ui.designsystem.component.AdditionalInfoItemColor
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.component.InfoBar
import org.hisp.dhis.mobile.ui.designsystem.component.parameter.ParameterSelectorItem
import org.hisp.dhis.mobile.ui.designsystem.theme.Radius
import org.hisp.dhis.mobile.ui.designsystem.theme.Shape
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor
import org.hisp.dhis.mobile.ui.designsystem.theme.TextColor
import timber.log.Timber

@Composable
fun SearchParametersScreen(
    resourceManager: ResourceManager,
    uiState: SearchParametersUiState,
    intentHandler: (FormIntent) -> Unit,
    onSimprintsBiometricIdentificationResult: (String, String?, Boolean) -> Unit,
    onSimprintsBiometricNoMatches: (String) -> Unit,
    onShowOrgUnit: (
        uid: String,
        preselectedOrgUnits: List<String>,
        orgUnitScope: OrgUnitSelectorScope,
        label: String,
    ) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current.applicationContext
    val configuration = LocalConfiguration.current
    var pendingSimprintsFieldUid by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSimprintsValueTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSimprintsResponseDataJson by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSimprintsCapturesSessionId by rememberSaveable { mutableStateOf(false) }

    val simprintsSessionRepository =
        remember(context) {
            Injector.provideSimprintsSessionRepository(context)
        }
    val simprintsHasAutoOpenEligibleIdentificationUseCase =
        remember {
            SimprintsHasAutoOpenEligibleIdentificationUseCase()
        }
    val simprintsIdentifyLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uid = pendingSimprintsFieldUid
            val valueType =
                pendingSimprintsValueTypeName
                    ?.let(ValueType::valueOf)
            val returnedValue =
                mapPendingSimprintsSearchResult(
                    responseDataJson = pendingSimprintsResponseDataJson,
                    resultCode = result.resultCode,
                    data = result.data,
                    capturesSessionId = pendingSimprintsCapturesSessionId,
                    sessionRepository = simprintsSessionRepository,
                )

            pendingSimprintsFieldUid = null
            pendingSimprintsValueTypeName = null
            pendingSimprintsResponseDataJson = null
            pendingSimprintsCapturesSessionId = false

            if (uid != null && result.resultCode == RESULT_OK && returnedValue != null) {
                onSimprintsBiometricIdentificationResult(
                    uid,
                    returnedValue,
                    simprintsHasAutoOpenEligibleIdentificationUseCase(result.data?.extras),
                )
                intentHandler(
                    FormIntent.OnSave(
                        uid = uid,
                        value = returnedValue,
                        valueType = valueType,
                    ),
                )
            } else if (uid != null && result.resultCode == RESULT_OK) {
                onSimprintsBiometricNoMatches(uid)
            }
        }

    val scanContract = remember { ScanContract() }
    val qrScanLauncher =
        rememberLauncherForActivityResult(
            contract = scanContract,
        ) { result ->
            result.contents?.let { qrData ->
                val intent =
                    FormIntent.OnQrCodeScanned(
                        uid = result.originalIntent.getStringExtra(Constants.UID)!!,
                        value = qrData,
                        valueType = ValueType.TEXT,
                    )
                intentHandler(intent)
            }
        }

    val callback =
        remember {
            object : FieldUiModel.Callback {
                override fun intent(intent: FormIntent) {
                    intentHandler.invoke(intent)
                }

                override fun recyclerViewUiEvents(uiEvent: RecyclerViewUiEvents) {
                    when (uiEvent) {
                        is RecyclerViewUiEvents.OpenOrgUnitDialog ->
                            onShowOrgUnit(
                                uiEvent.uid,
                                uiEvent.value?.let { listOf(it) } ?: emptyList(),
                                uiEvent.orgUnitSelectorScope ?: OrgUnitSelectorScope.UserSearchScope(),
                                uiEvent.label,
                            )

                        is RecyclerViewUiEvents.ScanQRCode -> {
                            qrScanLauncher.launch(
                                ScanOptions().apply {
                                    setDesiredBarcodeFormats()
                                    setPrompt("")
                                    setBeepEnabled(true)
                                    setBarcodeImageEnabled(false)
                                    addExtra(Constants.UID, uiEvent.uid)
                                    uiEvent.optionSet?.let {
                                        addExtra(
                                            Constants.OPTION_SET,
                                            uiEvent.optionSet,
                                        )
                                    }
                                    addExtra(Constants.SCAN_RENDERING_TYPE, uiEvent.renderingType)
                                },
                            )
                        }

                        else -> {
                            // no-op
                        }
                    }
                }
            }
        }

    uiState.minAttributesMessage?.let { message ->
        coroutineScope.launch {
            uiState.shouldShowMinAttributeWarning.collectLatest {
                if (it) {
                    snackBarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short,
                    )
                    uiState.updateMinAttributeWarning(false)
                }
            }
        }
    }

    LaunchedEffect(uiState.isOnBackPressed) {
        uiState.isOnBackPressed.collectLatest {
            if (it) {
                focusManager.clearFocus()
                onClose()
            }
        }
    }

    val backgroundShape =
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE ->
                RoundedCornerShape(
                    topStart = CornerSize(Radius.L),
                    topEnd = CornerSize(Radius.NoRounding),
                    bottomEnd = CornerSize(Radius.NoRounding),
                    bottomStart = CornerSize(Radius.NoRounding),
                )

            else -> Shape.LargeTop
        }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                modifier =
                    Modifier.padding(
                        start = 8.dp,
                        top = 8.dp,
                        end = 8.dp,
                        bottom = 48.dp,
                    ),
            )
        },
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(color = Color.White, shape = backgroundShape)
                    .padding(
                        top = 0.dp,
                        bottom = paddingValues.calculateBottomPadding(),
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                    ),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1F),
            ) {
                if (uiState.items.isEmpty()) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            InfoBar(
                                modifier = Modifier.testTag("EMPTY_SEARCH_ATTRIBUTES_TEXT_TAG"),
                                text = resourceManager.getString(R.string.empty_search_attributes_message),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "warning",
                                        tint = AdditionalInfoItemColor.WARNING.color,
                                    )
                                },
                                textColor = AdditionalInfoItemColor.WARNING.color,
                                backgroundColor = AdditionalInfoItemColor.WARNING.color.copy(alpha = 0.1f),
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.items,
                        key = { _, fieldUiModel ->
                            fieldUiModel.uid
                        },
                    ) { index, fieldUiModel ->
                        fieldUiModel.setCallback(callback)
                        ParameterSelectorItem(
                            modifier =
                                Modifier
                                    .testTag("SEARCH_PARAM_ITEM"),
                            model =
                                provideParameterSelectorItem(
                                    resources = resourceManager,
                                    focusManager = focusManager,
                                    fieldUiModel = fieldUiModel,
                                    callback = callback,
                                    onSimprintsBiometricIdentificationLaunch = {
                                        uid,
                                        valueType,
                                        responseDataJson,
                                        capturesSessionId,
                                        launchIntent,
                                        ->
                                        pendingSimprintsFieldUid = uid
                                        pendingSimprintsValueTypeName = valueType?.name
                                        pendingSimprintsResponseDataJson = responseDataJson
                                        pendingSimprintsCapturesSessionId = capturesSessionId
                                        simprintsIdentifyLauncher.launch(launchIntent)
                                    },
                                    onNextClicked = {
                                        val nextIndex = index + 1
                                        if (nextIndex < uiState.items.size) {
                                            uiState.items[nextIndex].onItemClick()
                                        }
                                    },
                                ),
                        )
                    }
                }

                if (uiState.clearSearchEnabled) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Button(
                                modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp),
                                style = ButtonStyle.TEXT,
                                text = resourceManager.getString(R.string.clear_search),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Cancel,
                                        contentDescription = resourceManager.getString(R.string.clear_search),
                                        tint = SurfaceColor.Primary,
                                    )
                                },
                            ) {
                                focusManager.clearFocus()
                                onClear()
                            }
                        }
                    }
                }
            }

            Button(
                enabled = uiState.searchEnabled,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp, 16.dp, 8.dp)
                        .testTag("SEARCH_BUTTON"),
                style = ButtonStyle.FILLED,
                text = resourceManager.getString(R.string.search),
                icon = {
                    val iconTint =
                        if (uiState.searchEnabled) {
                            TextColor.OnPrimary
                        } else {
                            TextColor.OnDisabledSurface
                        }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = null,
                        tint = iconTint,
                    )
                },
            ) {
                focusManager.clearFocus()
                onSearch()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchFormPreview() {
    SearchParametersScreen(
        resourceManager = ResourceManager(LocalContext.current, ColorUtils()),
        uiState =
            SearchParametersUiState(
                items =
                    buildList {
                        repeat(times = 20) { index ->
                            add(
                                FieldUiModelImpl(
                                    uid = "uid$index",
                                    label = "Label $index",
                                    autocompleteList = emptyList(),
                                    optionSetConfiguration = null,
                                    valueType = ValueType.TEXT,
                                ),
                            )
                        }
                    },
            ),
        intentHandler = {},
        onSimprintsBiometricIdentificationResult = { _, _, _ -> },
        onSimprintsBiometricNoMatches = { _ -> },
        onShowOrgUnit = { _, _, _, _ -> },
        onSearch = {},
        onClear = {},
        onClose = {},
    )
}

private fun mapPendingSimprintsSearchResult(
    responseDataJson: String?,
    resultCode: Int,
    data: Intent?,
    capturesSessionId: Boolean,
    sessionRepository: org.dhis2.commons.simprints.repository.SimprintsSessionRepository,
): String? {
    if (resultCode != RESULT_OK) {
        return null
    }

    if (capturesSessionId) {
        SimprintsIntentUtils.extractSessionId(data?.extras)?.let(sessionRepository::save)
    }

    val responseData =
        responseDataJson
            ?.let {
                try {
                    Gson().fromJson<List<CustomIntentResponseDataModel>>(
                        it,
                        object : TypeToken<List<CustomIntentResponseDataModel>>() {}.type,
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse CustomIntentResponseDataModel")
                    null
                }
            } ?: return null

    val returnedValue =
        CustomIntentActivityResultContract()
            .mapIntentResponseData(responseData, data)
            ?.takeUnless(List<String>::isEmpty)
            ?.joinToString(separator = ",") ?: return null

    return returnedValue
}

@Preview(showBackground = true)
@Composable
fun SearchFormPreviewWithClear() {
    SearchParametersScreen(
        resourceManager = ResourceManager(LocalContext.current, ColorUtils()),
        uiState =
            SearchParametersUiState(
                items =
                    buildList {
                        repeat(times = 20) { index ->
                            add(
                                FieldUiModelImpl(
                                    uid = "uid$index",
                                    label = "Label $index",
                                    value = "test value",
                                    autocompleteList = emptyList(),
                                    optionSetConfiguration = null,
                                    valueType = ValueType.TEXT,
                                ),
                            )
                        }
                    },
            ),
        intentHandler = {},
        onSimprintsBiometricIdentificationResult = { _, _, _ -> },
        onSimprintsBiometricNoMatches = { _ -> },
        onShowOrgUnit = { _, _, _, _ -> },
        onSearch = {},
        onClear = {},
        onClose = {},
    )
}

fun initSearchScreen(
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
        SearchParametersScreen(
            resourceManager = resources,
            uiState = viewModel.searchParametersUiState,
            onSearch = viewModel::onSearch,
            intentHandler = viewModel::onParameterIntent,
            onSimprintsBiometricIdentificationResult = viewModel::onSimprintsBiometricIdentificationResult,
            onSimprintsBiometricNoMatches = viewModel::onSimprintsBiometricNoMatches,
            onShowOrgUnit = onShowOrgUnit,
            onClear = {
                onClear()
                viewModel.clearQueryData()
                viewModel.clearFocus()
            },
            onClose = {
                viewModel.clearFocus()
            },
        )
    }
}
