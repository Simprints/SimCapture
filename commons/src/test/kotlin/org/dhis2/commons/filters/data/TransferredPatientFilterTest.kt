package org.dhis2.commons.filters.data

import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.ownership.ProgramOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferredPatientFilterTest {
    @Test
    fun `should compare selected program ownership with active enrollment org unit`() {
        val programUid = "programUid"
        val transferred = trackedEntity("transferred", programUid, "destination")
        val notTransferred = trackedEntity("notTransferred", programUid, "enrollment")
        val otherProgramOwnerFirst =
            TrackedEntityInstance
                .builder()
                .uid("multipleOwners")
                .programOwners(
                    listOf(
                        programOwner("multipleOwners", "otherProgram", "enrollment"),
                        programOwner("multipleOwners", programUid, "destination"),
                    ),
                ).build()

        val result =
            transferredTrackedEntityUids(
                programUid = programUid,
                enrollments =
                    listOf(
                        enrollment("transferred", "enrollment"),
                        enrollment("notTransferred", "enrollment"),
                        enrollment("multipleOwners", "enrollment"),
                        enrollment("multipleEnrollments", "completedOrgUnit", EnrollmentStatus.COMPLETED),
                        enrollment("multipleEnrollments", "activeOrgUnit"),
                    ),
                trackedEntities =
                    listOf(
                        transferred,
                        notTransferred,
                        otherProgramOwnerFirst,
                        trackedEntity("multipleEnrollments", programUid, "activeOrgUnit"),
                    ),
            )

        assertEquals(listOf("transferred", "multipleOwners"), result)
    }

    private fun enrollment(
        trackedEntityUid: String,
        orgUnitUid: String,
        status: EnrollmentStatus = EnrollmentStatus.ACTIVE,
    ): Enrollment =
        Enrollment
            .builder()
            .uid("enrollment-$trackedEntityUid")
            .trackedEntityInstance(trackedEntityUid)
            .program("programUid")
            .organisationUnit(orgUnitUid)
            .status(status)
            .attributeOptionCombo("attributeOptionCombo")
            .build()

    private fun trackedEntity(
        trackedEntityUid: String,
        programUid: String,
        ownerOrgUnitUid: String,
    ): TrackedEntityInstance =
        TrackedEntityInstance
            .builder()
            .uid(trackedEntityUid)
            .programOwners(listOf(programOwner(trackedEntityUid, programUid, ownerOrgUnitUid)))
            .build()

    private fun programOwner(
        trackedEntityUid: String,
        programUid: String,
        ownerOrgUnitUid: String,
    ): ProgramOwner =
        ProgramOwner
            .builder()
            .trackedEntityInstance(trackedEntityUid)
            .program(programUid)
            .ownerOrgUnit(ownerOrgUnitUid)
            .build()
}
