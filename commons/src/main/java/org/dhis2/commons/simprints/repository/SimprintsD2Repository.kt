package org.dhis2.commons.simprints.repository

import org.hisp.dhis.android.core.D2

class SimprintsD2Repository(
    private val d2: D2,
) {
    data class EnrollmentContext(
        val teiUid: String,
        val programUid: String?,
        val orgUnitUid: String?,
    )

    fun getEnrollmentContext(enrollmentUid: String): EnrollmentContext? =
        d2
            .enrollmentModule()
            .enrollments()
            .uid(enrollmentUid)
            .blockingGet()
            ?.let { enrollment ->
                EnrollmentContext(
                    teiUid = enrollment.trackedEntityInstance() ?: return null,
                    programUid = enrollment.program(),
                    orgUnitUid = enrollment.organisationUnit(),
                )
            }

    fun getProgramAttributeUids(programUid: String): List<String> =
        d2
            .programModule()
            .programTrackedEntityAttributes()
            .byProgram()
            .eq(programUid)
            .blockingGet()
            .mapNotNull { it.trackedEntityAttribute()?.uid() }

    fun getTrackedEntityTypeAttributeUids(teiUid: String): List<String> =
        d2
            .trackedEntityModule()
            .trackedEntityInstances()
            .uid(teiUid)
            .blockingGet()
            ?.trackedEntityType()
            ?.let { trackedEntityTypeUid ->
                d2
                    .trackedEntityModule()
                    .trackedEntityTypeAttributes()
                    .byTrackedEntityTypeUid()
                    .eq(trackedEntityTypeUid)
                    .blockingGet()
                    .mapNotNull { it.trackedEntityAttribute()?.uid() }
            } ?: emptyList()

    fun getTrackedEntityAttributeValue(
        teiUid: String,
        attributeUid: String,
    ): String? =
        d2
            .trackedEntityModule()
            .trackedEntityAttributeValues()
            .value(attributeUid, teiUid)
            .blockingGet()
            ?.value()

    fun saveTrackedEntityAttributeValue(
        teiUid: String,
        attributeUid: String,
        value: String,
    ) {
        d2
            .trackedEntityModule()
            .trackedEntityAttributeValues()
            .value(attributeUid, teiUid)
            .blockingSet(value)
    }
}
