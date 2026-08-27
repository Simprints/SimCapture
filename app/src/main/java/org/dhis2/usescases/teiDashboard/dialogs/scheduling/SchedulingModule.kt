package org.dhis2.usescases.teiDashboard.dialogs.scheduling

import dagger.Module
import dagger.Provides
import org.dhis2.commons.date.DateUtils
import org.dhis2.commons.simprints.ramp.repository.RampDatastoreRepository
import org.hisp.dhis.android.core.D2

@Module
class SchedulingModule {
    @Provides
    fun providesDateUtils(): DateUtils = DateUtils.getInstance()

    @Provides
    fun provideRampDatastoreRepository(d2: D2): RampDatastoreRepository = RampDatastoreRepository(d2)
}
