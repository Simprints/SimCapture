package org.dhis2.form.di

import android.content.Context
import org.dhis2.commons.R
import org.dhis2.commons.data.EntryMode
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.prefs.PreferenceProviderImpl
import org.dhis2.commons.resources.ColorUtils
import org.dhis2.commons.resources.DhisPeriodUtils
import org.dhis2.commons.resources.EventResourcesProvider
import org.dhis2.commons.resources.MetadataIconProvider
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.simprints.ramp.repository.RampDatastoreRepository as SimprintsRampDatastoreRepository
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.repository.SimprintsSessionRepository
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.form.data.DataEntryRepository
import org.dhis2.form.data.EnrollmentRepository
import org.dhis2.form.data.EventRepository
import org.dhis2.form.data.FormRepository
import org.dhis2.form.data.FormRepositoryImpl
import org.dhis2.form.data.FormValueStore
import org.dhis2.form.data.OptionsRepository
import org.dhis2.form.data.RulesUtilsProviderImpl
import org.dhis2.form.data.SearchOptionSetOption
import org.dhis2.form.data.metadata.EnrollmentConfiguration
import org.dhis2.form.data.metadata.FileResourceConfiguration
import org.dhis2.form.data.metadata.OptionSetConfiguration
import org.dhis2.form.data.metadata.OrgUnitConfiguration
import org.dhis2.form.model.EnrollmentRecords
import org.dhis2.form.model.EventRecords
import org.dhis2.form.model.FormRepositoryRecords
import org.dhis2.form.model.coroutine.FormDispatcher
import org.dhis2.form.simprints.ramp.data.FormHistoryChartRepository as SimprintsRampFormHistoryChartRepository
import org.dhis2.form.simprints.ramp.data.GetFormHistoryChartUseCase as GetSimprintsRampFormHistoryChartUseCase
import org.dhis2.form.ui.FieldViewModelFactory
import org.dhis2.form.ui.FieldViewModelFactoryImpl
import org.dhis2.form.ui.FormViewModelFactory
import org.dhis2.form.ui.mapper.FormSectionMapper
import org.dhis2.form.ui.provider.AutoCompleteProviderImpl
import org.dhis2.form.ui.provider.DisplayNameProviderImpl
import org.dhis2.form.ui.provider.EnrollmentFormLabelsProvider
import org.dhis2.form.ui.provider.FormResultDialogProvider
import org.dhis2.form.ui.provider.FormResultDialogResourcesProvider
import org.dhis2.form.ui.provider.HintProviderImpl
import org.dhis2.form.ui.provider.KeyboardActionProviderImpl
import org.dhis2.form.ui.provider.LegendValueProviderImpl
import org.dhis2.form.ui.provider.UiEventTypesProviderImpl
import org.dhis2.mobile.commons.customintents.CustomIntentRepository
import org.dhis2.mobile.commons.customintents.CustomIntentRepositoryImpl
import org.dhis2.mobile.commons.providers.FieldErrorMessageProvider
import org.dhis2.mobile.commons.reporting.CrashReportControllerImpl
import org.dhis2.mobileProgramRules.EvaluationType
import org.dhis2.mobileProgramRules.RuleEngineHelper
import org.dhis2.mobileProgramRules.RulesRepository
import org.hisp.dhis.android.core.D2Manager
import org.hisp.dhis.android.core.enrollment.EnrollmentObjectRepository
import org.hisp.dhis.android.core.event.EventObjectRepository

object Injector {
    fun provideFormViewModelFactory(
        context: Context,
        repositoryRecords: FormRepositoryRecords,
        programUid: String?,
        openErrorLocation: Boolean,
        useCompose: Boolean,
    ): FormViewModelFactory =
        FormViewModelFactory(
            provideFormRepository(
                context,
                repositoryRecords,
                programUid,
                useCompose,
            ),
            provideDispatchers(),
            openErrorLocation,
            provideFormResultDialogProvider(context),
            FormSectionMapper(),
        )

    private fun provideFormResultDialogProvider(context: Context) =
        FormResultDialogProvider(
            FormResultDialogResourcesProvider(
                provideResourcesManager(context),
            ),
        )

    private fun provideD2() = D2Manager.getD2()

    fun provideOptionSetDialog(): SearchOptionSetOption =
        SearchOptionSetOption(
            provideD2().optionModule().options(),
        )

    private fun provideMetadataIconProvider() = MetadataIconProvider(provideD2())

    fun provideDispatchers(): DispatcherProvider = FormDispatcher()

    fun provideCustomIntentProvider(programUid: String?): CustomIntentRepository {
        val d2 = provideD2()
        val rampDatastoreRepository = SimprintsRampDatastoreRepository(d2)
        return CustomIntentRepositoryImpl(d2) {
            rampDatastoreRepository.isOneLevelUpOrgUnitForBiometricsModuleId(programUid)
        }
    }

    private fun provideFormRepository(
        context: Context,
        repositoryRecords: FormRepositoryRecords,
        programUid: String?,
        useCompose: Boolean,
    ): FormRepository {
        val biometricsCaptureOnlyAttributeId by lazy {
            if (repositoryRecords.entryMode == EntryMode.ATTR) {
                SimprintsRampDatastoreRepository(provideD2())
                    .biometricsCaptureOnlyAttributeId(programUid)
            } else {
                null
            }
        }
        val biometricsCaptureOnlyAttributeIdProvider = { biometricsCaptureOnlyAttributeId }
        val externalCredentialAttributeId by lazy {
            if (repositoryRecords.entryMode == EntryMode.ATTR) {
                SimprintsRampDatastoreRepository(provideD2()).externalCredentialAttributeId(programUid)
            } else {
                null
            }
        }
        return FormRepositoryImpl(
            formValueStore =
                provideFormValueStore(
                    context = context,
                    recordUid = repositoryRecords.recordUid,
                    entryMode = repositoryRecords.entryMode,
                ),
            fieldErrorMessageProvider = provideFieldErrorMessage(),
            displayNameProvider = provideDisplayNameProvider(context),
            dataEntryRepository =
                provideDataEntryRepository(
                    entryMode = repositoryRecords.entryMode,
                    context = context,
                    repositoryRecords = repositoryRecords,
                    metadataIconProvider = provideMetadataIconProvider(),
                    customIntentRepository = provideCustomIntentProvider(programUid),
                    biometricsCaptureOnlyAttributeIdProvider = biometricsCaptureOnlyAttributeIdProvider,
                ),
            ruleEngineRepository =
                provideRuleEngineRepository(
                    repositoryRecords.entryMode,
                    repositoryRecords.recordUid,
                ),
            rulesUtilsProvider = provideRulesUtilsProvider(),
            legendValueProvider = provideLegendValueProvider(context),
            useCompose = useCompose,
            preferenceProvider = providePreferenceProvider(context),
            biometricsCaptureOnlyAttributeIdProvider = biometricsCaptureOnlyAttributeIdProvider,
            externalCredentialAttributeIdProvider = { externalCredentialAttributeId },
            simprintsD2Repository =
                if (repositoryRecords.entryMode == EntryMode.ATTR) SimprintsD2Repository(provideD2()) else null,
        )
    }

    private fun provideDataEntryRepository(
        entryMode: EntryMode?,
        context: Context,
        repositoryRecords: FormRepositoryRecords,
        metadataIconProvider: MetadataIconProvider,
        customIntentRepository: CustomIntentRepository,
        biometricsCaptureOnlyAttributeIdProvider: () -> String?,
    ): DataEntryRepository =
        when (entryMode) {
            EntryMode.ATTR ->
                provideEnrollmentRepository(
                    context,
                    repositoryRecords as EnrollmentRecords,
                    metadataIconProvider,
                    customIntentRepository,
                    biometricsCaptureOnlyAttributeIdProvider,
                )

            else ->
                provideEventRepository(
                    context,
                    repositoryRecords as EventRecords,
                    customIntentRepository,
                )
        }

    private fun provideEnrollmentRepository(
        context: Context,
        enrollmentRecords: EnrollmentRecords,
        metadataIconProvider: MetadataIconProvider,
        customIntentRepository: CustomIntentRepository,
        biometricsCaptureOnlyAttributeIdProvider: () -> String?,
    ): DataEntryRepository =
        EnrollmentRepository(
            fieldFactory = provideFieldFactory(context),
            conf =
                EnrollmentConfiguration(
                    provideD2(),
                    enrollmentRecords.enrollmentUid,
                    provideDispatchers(),
                ),
            enrollmentMode = enrollmentRecords.enrollmentMode,
            enrollmentFormLabelsProvider = provideEnrollmentFormLabelsProvider(context),
            metadataIconProvider = metadataIconProvider,
            customIntentRepository = customIntentRepository,
            biometricsCaptureOnlyAttributeIdProvider = biometricsCaptureOnlyAttributeIdProvider,
        )

    private fun provideEventRepository(
        context: Context,
        eventRecords: EventRecords,
        customIntentRepository: CustomIntentRepository,
    ): DataEntryRepository =
        EventRepository(
            fieldFactory = provideFieldFactory(context),
            eventUid = eventRecords.eventUid,
            d2 = provideD2(),
            metadataIconProvider = provideMetadataIconProvider(),
            resources = provideResourcesManager(context),
            eventResourcesProvider =
                EventResourcesProvider(
                    provideD2(),
                    provideResourcesManager(context),
                ),
            eventMode = eventRecords.eventMode,
            dispatcherProvider = provideDispatchers(),
            customIntentRepository = customIntentRepository,
            getSimprintsRampFormHistoryChart =
                GetSimprintsRampFormHistoryChartUseCase(
                    SimprintsRampDatastoreRepository(provideD2()),
                    SimprintsRampFormHistoryChartRepository(
                        eventUid = eventRecords.eventUid,
                        d2 = provideD2(),
                    ),
                ),
        )

    private fun provideEnrollmentFormLabelsProvider(context: Context) = EnrollmentFormLabelsProvider(provideResourcesManager(context))

    private fun provideFieldFactory(context: Context): FieldViewModelFactory =
        FieldViewModelFactoryImpl(
            hintProvider = provideHintProvider(context),
            displayNameProvider = provideDisplayNameProvider(context),
            uiEventTypesProvider = provideUiEventTypesProvider(),
            keyboardActionProvider = provideKeyBoardActionProvider(),
            legendValueProvider = provideLegendValueProvider(context),
            autoCompleteProvider = provideAutoCompleteProvider(context),
        )

    private fun provideKeyBoardActionProvider() = KeyboardActionProviderImpl()

    private fun provideUiEventTypesProvider() = UiEventTypesProviderImpl()

    private fun provideHintProvider(context: Context) = HintProviderImpl(context)

    private fun provideFormValueStore(
        context: Context,
        recordUid: String?,
        entryMode: EntryMode,
    ): FormValueStore {
        val enrollmentObjectRepository =
            if (entryMode == EntryMode.ATTR) {
                provideEnrollmentObjectRepository(recordUid!!)
            } else {
                null
            }

        val eventObjectRepository =
            if (entryMode == EntryMode.DE) {
                provideEventObjectRepository(recordUid!!)
            } else {
                null
            }

        return FormValueStore(
            d2 = provideD2(),
            recordUid =
                enrollmentObjectRepository?.blockingGet()?.trackedEntityInstance()
                    ?: recordUid!!,
            entryMode = entryMode,
            enrollmentRepository = enrollmentObjectRepository,
            eventRepository = eventObjectRepository,
            crashReportController = provideCrashReportController(context),
            networkUtils = provideNetworkUtils(context),
            resourceManager = provideResourcesManager(context),
        )
    }

    private fun provideEventObjectRepository(recordUid: String): EventObjectRepository = provideD2().eventModule().events().uid(recordUid)

    private fun provideEnrollmentObjectRepository(enrollmentUid: String): EnrollmentObjectRepository =
        provideD2().enrollmentModule().enrollments().uid(enrollmentUid)

    private fun provideCrashReportController(context: Context) = CrashReportControllerImpl(context, sentryDsn = "")

    private fun provideNetworkUtils(context: Context) = NetworkUtils(context)

    private fun provideResourcesManager(context: Context) =
        ResourceManager(
            context,
            provideColorUtils(),
        )

    private fun provideFieldErrorMessage() = FieldErrorMessageProvider()

    private fun provideDisplayNameProvider(context: Context) =
        DisplayNameProviderImpl(
            OptionSetConfiguration(provideD2()),
            OrgUnitConfiguration(provideD2()),
            FileResourceConfiguration(provideD2()),
            DhisPeriodUtils(
                d2 = provideD2(),
                defaultPeriodLabel = context.getString(R.string.period_span_default_label),
                defaultWeeklyLabel = context.getString(R.string.week_period_span_default_label),
                defaultBiWeeklyLabel = context.getString(R.string.biweek_period_span_default_label),
            ),
        )

    private fun providePreferenceProvider(context: Context) = PreferenceProviderImpl(context)

    fun provideSimprintsSessionRepository(context: Context) =
        SimprintsSessionRepository(providePreferenceProvider(context))

    private fun provideRuleEngineRepository(
        entryMode: EntryMode,
        recordUid: String,
    ): RuleEngineHelper =
        RuleEngineHelper(
            when (entryMode) {
                EntryMode.DE -> EvaluationType.Event(recordUid)
                EntryMode.ATTR -> EvaluationType.Enrollment(recordUid)
                else -> throw IllegalArgumentException()
            },
            RulesRepository(provideD2()),
        )

    private fun provideRulesUtilsProvider() =
        RulesUtilsProviderImpl(
            provideD2(),
            provideOptionsRepository(),
        )

    private fun provideOptionsRepository() = OptionsRepository(provideD2())

    private fun provideLegendValueProvider(context: Context) =
        LegendValueProviderImpl(
            provideD2(),
            provideResourcesManager(context),
        )

    private fun provideAutoCompleteProvider(context: Context) =
        AutoCompleteProviderImpl(
            providePreferenceProvider(context),
        )

    private fun provideColorUtils() = ColorUtils()
}
