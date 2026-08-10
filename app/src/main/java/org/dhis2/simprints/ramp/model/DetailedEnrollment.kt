package org.dhis2.simprints.ramp.model

import java.util.Date

data class DetailedEnrollment(
    val admitted: Date?,
    val discharge: Date?,
    val outcome: String?,
    val site: String?,
)
