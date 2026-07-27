package org.dhis2.simprints.ramp.data

import org.dhis2.commons.simprints.ramp.model.DetailedEnrollmentListingSettings
import org.dhis2.simprints.ramp.model.DetailedEnrollment
import org.dhis2.tracker.search.model.DomainEnrollment
import org.dhis2.tracker.search.model.DomainObjectStyle
import org.dhis2.tracker.search.model.DomainProgram
import org.dhis2.tracker.search.model.EnrollmentStatus
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.filters.internal.BooleanFilterConnector
import org.hisp.dhis.android.core.arch.repositories.filters.internal.StringFilterConnector
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.EventCollectionRepository
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitCollectionRepository
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date
import kotlin.time.Instant

class DetailedEnrollmentRepositoryTest {
    private val d2: D2 = Mockito.mock(D2::class.java, Mockito.RETURNS_DEEP_STUBS)
    private val organisationUnits: OrganisationUnitCollectionRepository = mock()
    private val organisationUnitFilter:
        StringFilterConnector<OrganisationUnitCollectionRepository> = mock()
    private val matchedOrganisationUnits: OrganisationUnitCollectionRepository = mock()
    private val events: EventCollectionRepository = mock()
    private val eventsWithDataValues: EventCollectionRepository = mock()
    private val enrollmentFilter: StringFilterConnector<EventCollectionRepository> = mock()
    private val enrollmentEvents: EventCollectionRepository = mock()
    private val deletedFilter: BooleanFilterConnector<EventCollectionRepository> = mock()
    private val activeEvents: EventCollectionRepository = mock()
    private val settings =
        DetailedEnrollmentListingSettings(
            dischargeOutcomeDataElementIds =
                setOf(OTHER_OUTCOME_DATA_ELEMENT_UID, OUTCOME_DATA_ELEMENT_UID),
            admissionProgramStageIds = setOf(ADMISSION_PROGRAM_STAGE_UID, OTHER_ADMISSION_STAGE_UID),
            dischargeProgramStageIds = setOf(DISCHARGE_PROGRAM_STAGE_UID, OTHER_DISCHARGE_STAGE_UID),
        )

    @Test
    fun `should return assembled enrollment data from d2`() {
        val admitted = Date(1_000)
        val discharge = Date(4_000)
        val enrollment =
            DomainEnrollment(
                uid = ENROLLMENT_UID,
                orgUnit = ORG_UNIT_UID,
                program = PROGRAM_UID,
                enrollmentDate = Instant.parse("2020-01-01T00:00:00Z"),
                incidentDate = null,
                completedDate = Instant.parse("2030-01-01T00:00:00Z"),
                followUp = false,
                status = EnrollmentStatus.COMPLETED,
                trackedEntityInstance = "tei",
            )
        val program =
            DomainProgram(
                uid = PROGRAM_UID,
                displayName = "OTP",
                style = DomainObjectStyle(icon = null, color = null),
            )
        stubOrganisationUnits()
        stubEvents()
        whenever(
            d2
                .dataElementModule()
                .dataElements()
                .uid(OUTCOME_DATA_ELEMENT_UID)
                .blockingGet(),
        ) doReturn
            DataElement
                .builder()
                .uid(OUTCOME_DATA_ELEMENT_UID)
                .categoryCombo(ObjectWithUid.create("category-combo"))
                .valueType(ValueType.TEXT)
                .build()

        val result =
            DetailedEnrollmentRepository(d2).get(
                enrollments = listOf(enrollment),
                programs = listOf(program),
                settings = settings,
            )

        assertEquals(
            listOf(
                DetailedEnrollment(
                    programName = "OTP",
                    admitted = admitted,
                    discharge = discharge,
                    outcome = "Cured",
                    site = "Site1",
                ),
            ),
            result,
        )
    }

    @Test
    fun `should return no details without enrollments`() {
        assertEquals(
            emptyList<DetailedEnrollment>(),
            DetailedEnrollmentRepository(d2).get(null, null, settings),
        )
    }

    @Test
    fun `should omit event details without configured ids`() {
        val enrollment =
            DomainEnrollment(
                uid = ENROLLMENT_UID,
                orgUnit = null,
                program = PROGRAM_UID,
                enrollmentDate = null,
                incidentDate = null,
                completedDate = null,
                followUp = false,
                status = EnrollmentStatus.ACTIVE,
                trackedEntityInstance = "tei",
            )
        val program =
            DomainProgram(
                uid = PROGRAM_UID,
                displayName = "OTP",
                style = DomainObjectStyle(icon = null, color = null),
            )

        assertEquals(
            listOf(
                DetailedEnrollment(
                    programName = "OTP",
                    admitted = null,
                    discharge = null,
                    outcome = null,
                    site = null,
                ),
            ),
            DetailedEnrollmentRepository(d2).get(
                enrollments = listOf(enrollment),
                programs = listOf(program),
                settings =
                    DetailedEnrollmentListingSettings(
                        dischargeOutcomeDataElementIds = emptySet(),
                        admissionProgramStageIds = emptySet(),
                        dischargeProgramStageIds = emptySet(),
                    ),
            ),
        )
    }

    @Test
    fun `should exclude enrollments in viewed program`() {
        val enrollment =
            DomainEnrollment(
                uid = ENROLLMENT_UID,
                orgUnit = ORG_UNIT_UID,
                program = PROGRAM_UID,
                enrollmentDate = null,
                incidentDate = null,
                completedDate = null,
                followUp = false,
                status = EnrollmentStatus.ACTIVE,
                trackedEntityInstance = "tei",
            )
        val program =
            DomainProgram(
                uid = PROGRAM_UID,
                displayName = "General Registration",
                style = DomainObjectStyle(icon = null, color = null),
            )

        assertEquals(
            emptyList<DetailedEnrollment>(),
            DetailedEnrollmentRepository(d2).get(
                enrollments = listOf(enrollment),
                programs = listOf(program),
                settings = settings,
                excludedProgramUid = PROGRAM_UID,
            ),
        )
    }

    private fun stubOrganisationUnits() {
        whenever(d2.organisationUnitModule().organisationUnits()) doReturn organisationUnits
        whenever(organisationUnits.byUid()) doReturn organisationUnitFilter
        whenever(organisationUnitFilter.`in`(listOf(ORG_UNIT_UID))) doReturn matchedOrganisationUnits
        whenever(matchedOrganisationUnits.blockingGet()) doReturn
            listOf(
                OrganisationUnit
                    .builder()
                    .uid(ORG_UNIT_UID)
                    .displayName("Site1")
                    .build(),
            )
    }

    private fun stubEvents() {
        whenever(d2.eventModule().events()) doReturn events
        whenever(events.withTrackedEntityDataValues()) doReturn eventsWithDataValues
        whenever(eventsWithDataValues.byEnrollmentUid()) doReturn enrollmentFilter
        whenever(enrollmentFilter.`in`(listOf(ENROLLMENT_UID))) doReturn enrollmentEvents
        whenever(enrollmentEvents.byDeleted()) doReturn deletedFilter
        whenever(deletedFilter.isFalse) doReturn activeEvents
        whenever(activeEvents.blockingGet()) doReturn
            listOf(
                event(
                    uid = "later-admission",
                    programStage = OTHER_ADMISSION_STAGE_UID,
                    date = Date(2_000),
                ),
                event(
                    uid = "earlier-admission",
                    programStage = ADMISSION_PROGRAM_STAGE_UID,
                    date = Date(1_000),
                ),
                event(
                    uid = "earlier-discharge",
                    programStage = OTHER_DISCHARGE_STAGE_UID,
                    date = Date(3_000),
                    outcome = "Relapse",
                ),
                event(
                    uid = "later-discharge",
                    programStage = DISCHARGE_PROGRAM_STAGE_UID,
                    date = Date(4_000),
                    outcome = "Cured",
                ),
                event(
                    uid = "ignored-stage",
                    programStage = "ignoredStage",
                    date = Date(5_000),
                    outcome = "Ignored",
                ),
            )
    }

    private fun event(
        uid: String,
        programStage: String,
        date: Date,
        outcome: String? = null,
    ): Event =
        Event
            .builder()
            .uid(uid)
            .enrollment(ENROLLMENT_UID)
            .programStage(programStage)
            .eventDate(date)
            .trackedEntityDataValues(
                outcome?.let {
                    listOf(
                        TrackedEntityDataValue
                            .builder()
                            .dataElement(OUTCOME_DATA_ELEMENT_UID)
                            .value(it)
                            .build(),
                    )
                }.orEmpty(),
            ).build()

    private companion object {
        const val ENROLLMENT_UID = "enrollment"
        const val PROGRAM_UID = "program"
        const val ORG_UNIT_UID = "org-unit"
        const val OUTCOME_DATA_ELEMENT_UID = "outcome1"
        const val OTHER_OUTCOME_DATA_ELEMENT_UID = "outcome2"
        const val ADMISSION_PROGRAM_STAGE_UID = "admissionStage1"
        const val OTHER_ADMISSION_STAGE_UID = "admissionStage2"
        const val DISCHARGE_PROGRAM_STAGE_UID = "dischargeStage1"
        const val OTHER_DISCHARGE_STAGE_UID = "dischargeStage2"
    }
}
