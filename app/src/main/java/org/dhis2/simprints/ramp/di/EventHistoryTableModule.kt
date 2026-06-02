package org.dhis2.simprints.ramp.di

import dagger.Module
import dagger.Provides
import org.dhis2.commons.di.dagger.PerFragment
import org.dhis2.commons.simprints.ramp.repository.RampDatastoreRepository
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.simprints.ramp.data.EventHistoryTableRepository
import org.dhis2.simprints.ramp.data.GetEventHistoryTableUseCase
import org.dhis2.simprints.ramp.ui.EventHistoryTableViewModelFactory
import org.hisp.dhis.android.core.D2

@Module
class EventHistoryTableModule(
    private val eventUid: String? = null,
    private val programUid: String? = null,
    private val enrollmentUid: String? = null,
) {
    @Provides
    @PerFragment
    fun provideSimprintsRampDatastoreRepository(d2: D2): RampDatastoreRepository = RampDatastoreRepository(d2)

    @Provides
    @PerFragment
    fun provideRepository(
        d2: D2,
        simprintsRampDatastoreRepository: RampDatastoreRepository,
    ): EventHistoryTableRepository =
        EventHistoryTableRepository(
            d2 = d2,
            simprintsRampDatastoreRepository = simprintsRampDatastoreRepository,
            eventUid = eventUid,
            programUid = programUid,
            enrollmentUid = enrollmentUid,
        )

    @Provides
    @PerFragment
    fun provideGetEventHistoryTableUseCase(repository: EventHistoryTableRepository): GetEventHistoryTableUseCase =
        GetEventHistoryTableUseCase(repository)

    @Provides
    @PerFragment
    fun provideViewModelFactory(
        getEventHistoryTable: GetEventHistoryTableUseCase,
        dispatcherProvider: DispatcherProvider,
    ): EventHistoryTableViewModelFactory =
        EventHistoryTableViewModelFactory(
            getEventHistoryTable = getEventHistoryTable,
            dispatcherProvider = dispatcherProvider,
        )
}
