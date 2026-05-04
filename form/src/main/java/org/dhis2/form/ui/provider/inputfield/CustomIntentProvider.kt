package org.dhis2.form.ui.provider.inputfield

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.form.R
import org.dhis2.form.di.Injector
import org.dhis2.form.extensions.inputState
import org.dhis2.form.extensions.supportingText
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.simprints.rememberSimprintsCustomIntentFormPresenter
import org.dhis2.form.ui.customintent.CustomIntentActivityResultContract
import org.dhis2.form.ui.customintent.CustomIntentInput
import org.dhis2.form.ui.customintent.CustomIntentResult
import org.dhis2.form.ui.event.RecyclerViewUiEvents
import org.dhis2.form.ui.intent.FormIntent
import org.hisp.dhis.mobile.ui.designsystem.component.CustomIntentState
import org.hisp.dhis.mobile.ui.designsystem.component.InputCustomIntent
import org.hisp.dhis.mobile.ui.designsystem.component.InputShellState
import org.hisp.dhis.mobile.ui.designsystem.component.InputStyle
import org.hisp.dhis.mobile.ui.designsystem.component.SupportingTextData
import org.hisp.dhis.mobile.ui.designsystem.component.SupportingTextState

@Composable
fun ProvideCustomIntentInput(
    fieldUiModel: FieldUiModel,
    resources: ResourceManager,
    intentHandler: (FormIntent) -> Unit,
    uiEventHandler: (RecyclerViewUiEvents) -> Unit,
    inputStyle: InputStyle,
    reEvaluateRequestParams: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current.applicationContext
    val values =
        remember(fieldUiModel) {
            fieldUiModel.value?.takeIf { it.isNotEmpty() }?.let { value ->
                mutableStateListOf(*value.split(",").toTypedArray())
            } ?: mutableStateListOf()
        }
    var customIntentState by remember(values, fieldUiModel) {
        mutableStateOf(getCustomIntentState(values, fieldUiModel.isLoadingData))
    }
    val errorGettingDataMessage =
        SupportingTextData(
            state = SupportingTextState.ERROR,
            text = resources.getString(R.string.custom_intent_error),
        )
    val fieldErrorMessage =
        SupportingTextData(
            state = SupportingTextState.ERROR,
            text = fieldUiModel.error ?: "",
        )
    val supportingTextList = remember { fieldUiModel.supportingText()?.toMutableList() ?: mutableListOf() }
    var inputShellState by remember { mutableStateOf(fieldUiModel.inputState()) }
    if (fieldUiModel.error != null) {
        inputShellState = InputShellState.ERROR
        customIntentState = CustomIntentState.LAUNCH
        if (!supportingTextList.contains(fieldErrorMessage)) supportingTextList.add(fieldErrorMessage)
    }
    val simprintsSessionRepository =
        remember(context) {
            Injector.provideSimprintsSessionRepository(context)
        }
    val simprintsCustomIntentFormPresenter =
        rememberSimprintsCustomIntentFormPresenter(
            fieldUiModel = fieldUiModel,
            resources = resources,
            sessionRepository = simprintsSessionRepository,
        )

    val lifecycleOwner = LocalLifecycleOwner.current
    var simprintsResumeCounter by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    simprintsResumeCounter++
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(
        fieldUiModel.value,
        fieldUiModel.isLoadingData,
        simprintsCustomIntentFormPresenter.hasPendingValue,
        simprintsResumeCounter,
    ) {
        val displayValues = simprintsCustomIntentFormPresenter.displayValues()
        if (values != displayValues) {
            values.clear()
            values.addAll(displayValues)
        }
        customIntentState = getCustomIntentState(values, fieldUiModel.isLoadingData)
    }
    val customIntentActivityResultContract = remember { CustomIntentActivityResultContract() }
    val launcher =
        rememberLauncherForActivityResult(contract = customIntentActivityResultContract) {
            when (it) {
                is CustomIntentResult.Error -> {
                    customIntentState = CustomIntentState.LAUNCH
                    inputShellState = InputShellState.ERROR
                    if (!supportingTextList.contains(errorGettingDataMessage)) {
                        supportingTextList.add(
                            errorGettingDataMessage,
                        )
                    }
                }
                is CustomIntentResult.Success -> {
                    customIntentState = CustomIntentState.LOADED
                    intentHandler(
                        FormIntent.OnSave(
                            it.fieldUid,
                            it.value,
                            fieldUiModel.valueType,
                        ),
                    )
                }
                is CustomIntentResult.PossibleDuplicates -> {
                    customIntentState = CustomIntentState.LAUNCH
                    inputShellState = InputShellState.ERROR
                    if (!supportingTextList.contains(errorGettingDataMessage)) {
                        supportingTextList.add(
                            errorGettingDataMessage,
                        )
                    }
                }
            }
        }
    InputCustomIntent(
        title = fieldUiModel.label,
        buttonText = resources.getString(R.string.custom_intent_launch),
        supportingText = supportingTextList.toList(),
        inputShellState = inputShellState,
        inputStyle = inputStyle,
        modifier = modifier,
        isRequired = fieldUiModel.mandatory,
        onLaunch = {
            simprintsCustomIntentFormPresenter.prepareLaunch()

            if (simprintsCustomIntentFormPresenter.handlesLaunch) {
                customIntentState = CustomIntentState.LOADING
                if (supportingTextList.contains(errorGettingDataMessage)) {
                    supportingTextList.remove(errorGettingDataMessage)
                }
                uiEventHandler.invoke(
                    RecyclerViewUiEvents.LaunchCustomIntent(
                        fieldUiModel.customIntent,
                        fieldUiModel.uid,
                    ),
                )
                return@InputCustomIntent
            }

            if (reEvaluateRequestParams) {
                customIntentState = CustomIntentState.LOADING
                uiEventHandler.invoke(
                    RecyclerViewUiEvents.LaunchCustomIntent(
                        fieldUiModel.customIntent,
                        fieldUiModel.uid,
                    ),
                )
            } else {
                customIntentState = CustomIntentState.LOADING
                if (supportingTextList.contains(errorGettingDataMessage)) {
                    supportingTextList.remove(errorGettingDataMessage)
                }
                fieldUiModel.customIntent?.let {
                    launcher.launch(
                        CustomIntentInput(
                            fieldUid = fieldUiModel.uid,
                            customIntent = it,
                            defaultTitle = fieldUiModel.customIntent?.name ?: resources.getString(R.string.select_app_intent),
                        ),
                    )
                }
            }
        },
        onClear = {
            simprintsCustomIntentFormPresenter.clearPendingValue()
            values.clear()
            intentHandler(FormIntent.ClearValue(fieldUiModel.uid))
        },
        customIntentState = customIntentState,
        values = values.toList(),
    )
}

fun getCustomIntentState(
    values: SnapshotStateList<String>,
    isLoading: Boolean,
): CustomIntentState =
    if (values.isEmpty()) {
        CustomIntentState.LAUNCH
    } else if (isLoading) {
        CustomIntentState.LOADING
    } else {
        CustomIntentState.LOADED
    }
