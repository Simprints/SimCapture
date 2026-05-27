package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.history

import dagger.Module
import dagger.Provides
import org.dhis2.commons.di.dagger.PerFragment
import org.hisp.dhis.android.core.D2

@Module
class EventHistoryTableModule(
    private val eventUid: String? = null,
    private val programUid: String? = null,
    private val enrollmentUid: String? = null,
) {
    @Provides
    @PerFragment
    fun provideRepository(d2: D2): EventHistoryTableRepository =
        EventHistoryTableRepository(
            d2 = d2,
            eventUid = eventUid,
            programUid = programUid,
            enrollmentUid = enrollmentUid,
        )
}
