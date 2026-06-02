package org.dhis2.simprints.ramp.di

import dagger.Subcomponent
import org.dhis2.commons.di.dagger.PerFragment
import org.dhis2.simprints.ramp.ui.EventHistoryTableFragment

@PerFragment
@Subcomponent(modules = [EventHistoryTableModule::class])
interface EventHistoryTableComponent {
    fun inject(fragment: EventHistoryTableFragment)
}
