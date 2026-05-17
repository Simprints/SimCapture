package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.history

import dagger.Subcomponent
import org.dhis2.commons.di.dagger.PerFragment

@PerFragment
@Subcomponent(modules = [EventHistoryTableModule::class])
interface EventHistoryTableComponent {
    fun inject(fragment: EventHistoryTableFragment)
}
