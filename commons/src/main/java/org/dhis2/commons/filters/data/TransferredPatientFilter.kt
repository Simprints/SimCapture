package org.dhis2.commons.filters.data

import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance

internal fun transferredTrackedEntityUids(
    programUid: String,
    enrollments: List<Enrollment>,
    trackedEntities: List<TrackedEntityInstance>,
): List<String> {
    val enrollmentOrgUnitByTrackedEntity =
        enrollments
            .groupBy { it.trackedEntityInstance() }
            .mapValues { (_, values) ->
                (values.firstOrNull { it.status() == EnrollmentStatus.ACTIVE } ?: values.first()).organisationUnit()
            }

    return trackedEntities.mapNotNull { trackedEntity ->
        val enrollmentOrgUnit = enrollmentOrgUnitByTrackedEntity[trackedEntity.uid()] ?: return@mapNotNull null
        val ownerOrgUnit =
            trackedEntity
                .programOwners()
                ?.firstOrNull { it.program() == programUid }
                ?.ownerOrgUnit()
        trackedEntity.uid().takeIf {
            ownerOrgUnit != null && ownerOrgUnit != enrollmentOrgUnit
        }
    }
}
