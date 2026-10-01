package org.dhis2.usescases.eventsWithoutRegistration.eventDetails.data

import org.dhis2.commons.data.EventCreationType
import org.dhis2.form.ui.FieldViewModelFactory
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.filters.internal.BooleanFilterConnector
import org.hisp.dhis.android.core.arch.repositories.filters.internal.StringFilterConnector
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope.OrderByDirection.DESC
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.EventCollectionRepository
import org.hisp.dhis.android.core.event.EventStatus
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit
import org.hisp.dhis.android.core.program.ProgramStage
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

class EventDetailsRepositoryTest {
    private val date: Date = mock()
    private val event: Event =
        mock {
            on { eventDate() } doReturn date
            on { dueDate() } doReturn date
        }
    private val programStage: ProgramStage = mock()
    private val d2: D2 = Mockito.mock(D2::class.java, Mockito.RETURNS_DEEP_STUBS)

    private val fieldViewModelFactory: FieldViewModelFactory = mock()
    private lateinit var repository: EventDetailsRepository

    @Before
    fun setup() {
        repository =
            EventDetailsRepository(
                d2,
                PROGRAM_UID,
                EVENT_UID,
                PROGRAM_STAGE_UID,
                fieldViewModelFactory,
                EventCreationType.ADDNEW,
            ) { d2Error -> "" }

        whenever(
            d2
                .programModule()
                .programStages()
                .uid(PROGRAM_STAGE_UID)
                .blockingGet(),
        ) doReturn programStage

        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID),
        ) doReturn mock()
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid(),
        ) doReturn mock()
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID),
        ) doReturn mock()
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID)
                .byDeleted(),
        ) doReturn mock()
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID)
                .byDeleted()
                .isFalse,
        ) doReturn mock()
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID)
                .byDeleted()
                .isFalse
                .orderByEventDate(DESC),
        ) doReturn mock()

        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID)
                .byDeleted()
                .isFalse
                .orderByDueDate(DESC),
        ) doReturn mock()
    }

    @Test
    fun `should getMinDaysFromStartByProgramStage`() {
        // Given a program stage with min days from start
        whenever(programStage.minDaysFromStart()) doReturn 1

        // When client is asking min days from start
        // Then is returning days from program stage
        assertEquals(repository.getMinDaysFromStartByProgramStage(), 1)
    }

    @Test
    fun `should not getMinDaysFromStartByProgramStage`() {
        // Given a program stage without min days from start
        whenever(programStage.minDaysFromStart()) doReturn null

        // When client is asking min days from start
        // Then is returning days from program stage
        assertEquals(repository.getMinDaysFromStartByProgramStage(), 0)
    }

    @Test
    fun `should getStateLastDate`() {
        // Given a program stage with active events
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID)
                .byDeleted()
                .isFalse
                .orderByEventDate(DESC)
                .blockingGet(),
        ) doReturn listOf(event)
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID)
                .byDeleted()
                .isFalse
                .orderByDueDate(DESC)
                .blockingGet(),
        ) doReturn emptyList()

        // When client is asking for getStageLastDate
        // Then gets the date of the active event
        assertEquals(repository.getStageLastDate(ENROLLMENT_UID), date)
    }

    @Test
    fun `should getStateLastDate for scheduled event`() {
        // Given a program stage with active events
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID)
                .byDeleted()
                .isFalse
                .orderByEventDate(DESC)
                .blockingGet(),
        ) doReturn emptyList()
        whenever(
            d2
                .eventModule()
                .events()
                .byEnrollmentUid()
                .eq(ENROLLMENT_UID)
                .byProgramStageUid()
                .eq(PROGRAM_STAGE_UID)
                .byDeleted()
                .isFalse
                .orderByDueDate(DESC)
                .blockingGet(),
        ) doReturn listOf(event)

        // When client is asking for getStageLastDate
        // Then gets the date of the active event
        assertEquals(repository.getStageLastDate(ENROLLMENT_UID), date)
    }

    @Test
    fun `Simprints RAMP anchored schedule context should advance for skipped visits after latest factual visit`() {
        val initialVisitDate = Date(1_000)
        val visit0 = stageEvent("visit0", Date(10), EventStatus.COMPLETED, initialVisitDate, 0)
        val oldSkippedVisit = stageEvent("oldSkip", Date(15), EventStatus.SKIPPED)
        val visit1 = stageEvent("visit1", Date(20), EventStatus.COMPLETED, Date(2_000), 1)
        val skippedVisit2 = stageEvent("skip2", Date(30), EventStatus.SKIPPED)
        val skippedVisit3 = stageEvent("skip3", Date(40), EventStatus.SKIPPED)
        stubAnchoredScheduleEvents(
            listOf(
                skippedVisit3,
                visit0,
                oldSkippedVisit,
                skippedVisit2,
                visit1,
            ),
        )

        val context =
            repository.getAnchoredScheduleContext(
                ENROLLMENT_UID,
                VISIT_NUMBER_DATA_ELEMENT_UID,
            )

        assertEquals(initialVisitDate, context?.initialVisitDate)
        assertEquals(3, context?.currentVisitNumber)
    }

    @Test
    fun `Simprints RAMP anchored schedule context should require factual visit zero`() {
        stubAnchoredScheduleEvents(
            listOf(
                stageEvent("visit1", Date(20), EventStatus.COMPLETED, Date(2_000), 1),
            ),
        )

        val context =
            repository.getAnchoredScheduleContext(
                ENROLLMENT_UID,
                VISIT_NUMBER_DATA_ELEMENT_UID,
            )

        assertEquals(null, context)
    }

    @Test
    fun `should use search scope in referral`() {
        whenever(
            d2
                .organisationUnitModule()
                .organisationUnits()
                .byOrganisationUnitScope(any()),
        ) doReturn mock()
        whenever(
            d2
                .organisationUnitModule()
                .organisationUnits()
                .byOrganisationUnitScope(any())
                .byProgramUids(any()),
        ) doReturn mock()

        whenever(
            d2
                .organisationUnitModule()
                .organisationUnits()
                .byProgramUids(any()),
        ) doReturn mock()

        whenever(
            d2
                .organisationUnitModule()
                .organisationUnits()
                .byProgramUids(any())
                .blockingGet(),
        ) doReturn listOf()

        whenever(
            d2
                .organisationUnitModule()
                .organisationUnits()
                .byOrganisationUnitScope(any())
                .byProgramUids(any())
                .blockingGet(),
        ) doReturn listOf()

        EventCreationType.entries.forEach { eventCreationType ->
            repository =
                EventDetailsRepository(
                    d2,
                    PROGRAM_UID,
                    EVENT_UID,
                    PROGRAM_STAGE_UID,
                    fieldViewModelFactory,
                    eventCreationType,
                ) { d2Error -> "" }

            repository.getOrganisationUnits()
        }

        verify(
            d2.organisationUnitModule().organisationUnits(),
            times(1),
        ).byProgramUids(listOf(PROGRAM_UID))

        verify(
            d2.organisationUnitModule().organisationUnits(),
            times(3),
        ).byOrganisationUnitScope(OrganisationUnit.Scope.SCOPE_DATA_CAPTURE)
    }

    private fun stubAnchoredScheduleEvents(events: List<Event>) {
        val eventRepository: EventCollectionRepository = mock()
        val eventsWithDataValues: EventCollectionRepository = mock()
        val enrollmentFilter: StringFilterConnector<EventCollectionRepository> = mock()
        val enrollmentEvents: EventCollectionRepository = mock()
        val programStageFilter: StringFilterConnector<EventCollectionRepository> = mock()
        val programStageEvents: EventCollectionRepository = mock()
        val deletedFilter: BooleanFilterConnector<EventCollectionRepository> = mock()
        val activeEvents: EventCollectionRepository = mock()

        whenever(d2.eventModule().events()) doReturn eventRepository
        whenever(eventRepository.withTrackedEntityDataValues()) doReturn eventsWithDataValues
        whenever(eventsWithDataValues.byEnrollmentUid()) doReturn enrollmentFilter
        whenever(enrollmentFilter.eq(ENROLLMENT_UID)) doReturn enrollmentEvents
        whenever(enrollmentEvents.byProgramStageUid()) doReturn programStageFilter
        whenever(programStageFilter.eq(PROGRAM_STAGE_UID)) doReturn programStageEvents
        whenever(programStageEvents.byDeleted()) doReturn deletedFilter
        whenever(deletedFilter.isFalse) doReturn activeEvents
        whenever(activeEvents.blockingGet()) doReturn events
    }

    private fun stageEvent(
        uid: String,
        createdAtClient: Date,
        status: EventStatus,
        eventDate: Date? = null,
        visitNumber: Int? = null,
    ): Event {
        val event: Event = mock()
        val dataValues =
            visitNumber?.let { visitNumber ->
                val visitNumberValue = visitNumber.toString()
                listOf(
                    mock<TrackedEntityDataValue> {
                        on { dataElement() } doReturn VISIT_NUMBER_DATA_ELEMENT_UID
                        on { value() } doReturn visitNumberValue
                    },
                )
            }.orEmpty()
        whenever(event.uid()) doReturn uid
        whenever(event.createdAtClient()) doReturn createdAtClient
        whenever(event.status()) doReturn status
        whenever(event.eventDate()) doReturn eventDate
        whenever(event.trackedEntityDataValues()) doReturn dataValues
        return event
    }

    companion object {
        const val PROGRAM_UID = "programUid"
        const val EVENT_UID = "eventUid"
        const val PROGRAM_STAGE_UID = "programStageUid"
        const val ENROLLMENT_UID = "enrollmentUid"
        const val VISIT_NUMBER_DATA_ELEMENT_UID = "visitNumberDataElementUid"
    }
}
