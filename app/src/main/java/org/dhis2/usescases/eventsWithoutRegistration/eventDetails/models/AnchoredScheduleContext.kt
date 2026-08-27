package org.dhis2.usescases.eventsWithoutRegistration.eventDetails.models

import java.util.Date

data class AnchoredScheduleContext(
    val initialVisitDate: Date,
    val currentVisitNumber: Int,
)
