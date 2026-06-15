package org.dhis2.simprints.ramp.data

import org.dhis2.commons.simprints.ramp.model.ProgramStageHistoryTableConfig
import org.dhis2.commons.simprints.ramp.model.RampDatastoreConfig
import org.dhis2.commons.simprints.ramp.repository.RampDatastoreRepository
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.filters.internal.BooleanFilterConnector
import org.hisp.dhis.android.core.arch.repositories.filters.internal.StringFilterConnector
import org.hisp.dhis.android.core.arch.repositories.`object`.ReadOnlyOneObjectRepositoryFinalImpl
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.EventCollectionRepository
import org.hisp.dhis.android.core.option.Option
import org.hisp.dhis.android.core.option.OptionCollectionRepository
import org.hisp.dhis.android.core.program.ProgramStageDataElement
import org.hisp.dhis.android.core.program.ProgramStageDataElementCollectionRepository
import org.hisp.dhis.android.core.program.ProgramStageSection
import org.hisp.dhis.android.core.program.ProgramStageSectionsCollectionRepository
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date

class EventHistoryTableRepositoryTest {
    private val d2: D2 = Mockito.mock(D2::class.java, Mockito.RETURNS_DEEP_STUBS)
    private val simprintsRampDatastoreRepository: RampDatastoreRepository = mock()
    private val events: EventCollectionRepository = mock()
    private val eventsWithDataValues: EventCollectionRepository = mock()
    private val eventUidFilter: StringFilterConnector<EventCollectionRepository> = mock()
    private val currentEventCollection: EventCollectionRepository = mock()
    private val currentEventRepository: ReadOnlyOneObjectRepositoryFinalImpl<Event> = mock()
    private val enrollmentFilter: StringFilterConnector<EventCollectionRepository> = mock()
    private val enrollmentEvents: EventCollectionRepository = mock()
    private val programStageFilter: StringFilterConnector<EventCollectionRepository> = mock()
    private val followUpEvents: EventCollectionRepository = mock()
    private val deletedFilter: BooleanFilterConnector<EventCollectionRepository> = mock()
    private val activeFollowUpEvents: EventCollectionRepository = mock()
    private val programStageDataElements: ProgramStageDataElementCollectionRepository = mock()
    private val programStageDataElementsWithRenderType: ProgramStageDataElementCollectionRepository = mock()
    private val programStageDataElementProgramStageFilter:
        StringFilterConnector<ProgramStageDataElementCollectionRepository> = mock()
    private val programStageDataElementsByStage: ProgramStageDataElementCollectionRepository = mock()
    private val sortedProgramStageDataElements: ProgramStageDataElementCollectionRepository = mock()
    private val programStageSections: ProgramStageSectionsCollectionRepository = mock()
    private val programStageSectionFilter: StringFilterConnector<ProgramStageSectionsCollectionRepository> = mock()
    private val programStageSectionsByStage: ProgramStageSectionsCollectionRepository = mock()
    private val programStageSectionsWithDataElements: ProgramStageSectionsCollectionRepository = mock()
    private val options: OptionCollectionRepository = mock()
    private val optionSetFilter: StringFilterConnector<OptionCollectionRepository> = mock()
    private val optionsBySet: OptionCollectionRepository = mock()

    @Test
    fun `table should map configured follow-up events into fixed visit columns`() {
        stubRampConfig()
        stubCurrentEvent(
            event(
                uid = CURRENT_EVENT_UID,
                eventDate = Date(2_000),
                values =
                    listOf(
                        value(VISIT_NUMBER_UID, "1"),
                        value(WEIGHT_UID, "9.0"),
                        value(STATUS_UID, "A"),
                        value(BOOLEAN_UID, "true"),
                    ),
            ),
        )
        stubRowsAndSections()
        stubFollowUpEvents(
            listOf(
                event(
                    uid = "previous",
                    eventDate = Date(1_000),
                    values =
                        listOf(
                            value(VISIT_NUMBER_UID, "0"),
                            value(WEIGHT_UID, "8.0"),
                            value(STATUS_UID, "A"),
                            value(BOOLEAN_UID, "true"),
                        ),
                ),
                event(
                    uid = "previous-duplicate",
                    eventDate = Date(1_500),
                    values =
                        listOf(
                            value(VISIT_NUMBER_UID, "0"),
                            value(WEIGHT_UID, "8.5"),
                            value(STATUS_UID, "B"),
                            value(BOOLEAN_UID, "false"),
                        ),
                ),
                event(
                    uid = "future",
                    eventDate = Date(3_000),
                    values =
                        listOf(
                            value(VISIT_NUMBER_UID, "2"),
                            value(WEIGHT_UID, "10.0"),
                            value(STATUS_UID, "A"),
                            value(BOOLEAN_UID, "true"),
                        ),
                ),
            ),
        )
        stubOptions()

        val table =
            EventHistoryTableRepository(
                d2 = d2,
                simprintsRampDatastoreRepository = simprintsRampDatastoreRepository,
                eventUid = CURRENT_EVENT_UID,
            ).getTable()

        assertEquals(listOf("0", "1", "2", "3"), table?.columns?.map { it.label })
        assertEquals("previous-duplicate", table?.columns?.get(0)?.eventUid)
        assertEquals(CURRENT_EVENT_UID, table?.columns?.get(1)?.eventUid)
        assertNull(table?.columns?.get(2)?.eventUid)
        assertEquals(listOf("Follow up"), table?.sections?.map { it.title })
        assertEquals(
            listOf("Weight", "Status", "Confirmed"),
            table
                ?.sections
                ?.single()
                ?.rows
                ?.map { it.label },
        )
        assertEquals(
            listOf("8.5", "9.0", "", ""),
            table
                ?.sections
                ?.single()
                ?.rows
                ?.get(0)
                ?.values
                ?.map { it.value },
        )
        // Visit 0 keeps the later duplicate event with OptionB
        assertEquals(
            listOf("OptionB", "OptionA", "", ""),
            table
                ?.sections
                ?.single()
                ?.rows
                ?.get(1)
                ?.values
                ?.map { it.value },
        )
        val booleanRowValues =
            table
                ?.sections
                ?.single()
                ?.rows
                ?.get(2)
                ?.values
        assertEquals(
            listOf("No", "Yes", "", ""),
            booleanRowValues?.map { it.displayValue(yesLabel = "Yes", noLabel = "No") },
        )
    }

    @Test
    fun `table should include all enrollment follow-up events when opened from dashboard`() {
        stubRampConfig()
        stubRowsAndSections()
        stubFollowUpEvents(
            listOf(
                event(
                    uid = "visit-two",
                    eventDate = Date(3_000),
                    values =
                        listOf(
                            value(VISIT_NUMBER_UID, "2"),
                            value(WEIGHT_UID, "10.0"),
                        ),
                ),
            ),
        )
        stubOptions()

        val table =
            EventHistoryTableRepository(
                d2 = d2,
                simprintsRampDatastoreRepository = simprintsRampDatastoreRepository,
                programUid = PROGRAM_UID,
                enrollmentUid = ENROLLMENT_UID,
            ).getTable()

        assertEquals("visit-two", table?.columns?.get(2)?.eventUid)
        assertEquals(
            "10.0",
            table
                ?.sections
                ?.single()
                ?.rows
                ?.first()
                ?.values
                ?.map { it.value }
                ?.get(2),
        )
    }

    @Test
    fun `table should exclude locally deleted follow-up events`() {
        stubRampConfig()
        stubRowsAndSections()
        stubFollowUpEvents(
            events =
                listOf(
                    event(
                        uid = "active",
                        eventDate = Date(1_000),
                        values =
                            listOf(
                                value(VISIT_NUMBER_UID, "0"),
                                value(WEIGHT_UID, "8.0"),
                            ),
                    ),
                ),
            unfilteredEvents =
                listOf(
                    event(
                        uid = "deleted",
                        eventDate = Date(1_000),
                        values =
                            listOf(
                                value(VISIT_NUMBER_UID, "0"),
                                value(WEIGHT_UID, "99.0"),
                            ),
                    ).toBuilder().deleted(true).build(),
                ),
        )
        stubOptions()

        val table =
            EventHistoryTableRepository(
                d2 = d2,
                simprintsRampDatastoreRepository = simprintsRampDatastoreRepository,
                programUid = PROGRAM_UID,
                enrollmentUid = ENROLLMENT_UID,
            ).getTable()

        assertEquals("active", table?.columns?.get(0)?.eventUid)
        assertEquals(
            "8.0",
            table
                ?.sections
                ?.single()
                ?.rows
                ?.first()
                ?.values
                ?.first()
                ?.value,
        )
    }

    @Test
    fun `table should follow section form order for sections and rows`() {
        stubRampConfig()
        stubRowsAndSections(
            programStageDataElementUids =
                listOf(
                    VISIT_NUMBER_UID,
                    WEIGHT_UID,
                    STATUS_UID,
                    BOOLEAN_UID,
                    EXCLUDED_UID,
                ),
            sections =
                listOf(
                    TestSectionDefinition(
                        uid = "second-section",
                        displayName = "Second section",
                        sortOrder = 2,
                        dataElementUids = listOf(BOOLEAN_UID),
                    ),
                    TestSectionDefinition(
                        uid = "first-section",
                        displayName = "First section",
                        sortOrder = 1,
                        dataElementUids = listOf(STATUS_UID, WEIGHT_UID),
                    ),
                ),
        )
        stubFollowUpEvents(emptyList())
        stubOptions()

        val table =
            EventHistoryTableRepository(
                d2 = d2,
                simprintsRampDatastoreRepository = simprintsRampDatastoreRepository,
                programUid = PROGRAM_UID,
                enrollmentUid = ENROLLMENT_UID,
            ).getTable()

        assertEquals(listOf("First section", "Second section"), table?.sections?.map { it.title })
        assertEquals(
            listOf("Status", "Weight"),
            table
                ?.sections
                ?.first()
                ?.rows
                ?.map { it.label },
        )
        assertEquals(
            listOf("Confirmed"),
            table
                ?.sections
                ?.get(1)
                ?.rows
                ?.map { it.label },
        )
    }

    private fun stubRampConfig() {
        whenever(simprintsRampDatastoreRepository.getConfig()) doReturn
            RampDatastoreConfig(
                programStageHistoryTables =
                    listOf(
                        ProgramStageHistoryTableConfig(
                            programId = PROGRAM_UID,
                            followUpVisitProgramStageId = PROGRAM_STAGE_UID,
                            followUpVisitMaxNumber = 3,
                            headerVisitNumberDataElementId = VISIT_NUMBER_UID,
                            excludedFollowUpVisitDataElementIds = listOf(EXCLUDED_UID),
                        ),
                    ),
            )
    }

    private fun stubCurrentEvent(event: Event) {
        whenever(d2.eventModule().events()) doReturn events
        whenever(events.withTrackedEntityDataValues()) doReturn eventsWithDataValues
        whenever(eventsWithDataValues.byUid()) doReturn eventUidFilter
        whenever(eventUidFilter.eq(CURRENT_EVENT_UID)) doReturn currentEventCollection
        whenever(currentEventCollection.one()) doReturn currentEventRepository
        whenever(currentEventRepository.blockingGet()) doReturn event
    }

    private fun stubFollowUpEvents(
        events: List<Event>,
        unfilteredEvents: List<Event> = events,
    ) {
        whenever(d2.eventModule().events()) doReturn this.events
        whenever(this.events.withTrackedEntityDataValues()) doReturn eventsWithDataValues
        whenever(eventsWithDataValues.byEnrollmentUid()) doReturn enrollmentFilter
        whenever(enrollmentFilter.eq(ENROLLMENT_UID)) doReturn enrollmentEvents
        whenever(enrollmentEvents.byProgramStageUid()) doReturn programStageFilter
        whenever(programStageFilter.eq(PROGRAM_STAGE_UID)) doReturn followUpEvents
        whenever(followUpEvents.blockingGet()) doReturn unfilteredEvents
        whenever(followUpEvents.byDeleted()) doReturn deletedFilter
        whenever(deletedFilter.isFalse) doReturn activeFollowUpEvents
        whenever(activeFollowUpEvents.blockingGet()) doReturn events
    }

    private fun stubRowsAndSections(
        programStageDataElementUids: List<String> =
            listOf(
                VISIT_NUMBER_UID,
                WEIGHT_UID,
                STATUS_UID,
                BOOLEAN_UID,
                EXCLUDED_UID,
            ),
        sections: List<TestSectionDefinition> =
            listOf(
                TestSectionDefinition(
                    uid = "section",
                    displayName = "Follow up",
                    sortOrder = 1,
                    dataElementUids = listOf(WEIGHT_UID, STATUS_UID, BOOLEAN_UID, EXCLUDED_UID),
                ),
                TestSectionDefinition(
                    uid = "empty-section",
                    displayName = "Hidden",
                    sortOrder = 2,
                    dataElementUids = listOf(EXCLUDED_UID),
                ),
            ),
    ) {
        val programStage = ObjectWithUid.create(PROGRAM_STAGE_UID)
        val weightDataElement = dataElement(WEIGHT_UID)
        val statusDataElement = dataElement(STATUS_UID)
        val booleanDataElement = dataElement(BOOLEAN_UID)
        val excludedDataElement = dataElement(EXCLUDED_UID)
        val visitNumberDataElement = dataElement(VISIT_NUMBER_UID)
        val dataElementsByUid =
            listOf(
                weightDataElement,
                statusDataElement,
                booleanDataElement,
                excludedDataElement,
                visitNumberDataElement,
            ).associateBy { dataElement -> dataElement.uid().orEmpty() }
        val programStageDataElementsForStage =
            programStageDataElementUids.map { dataElementUid ->
                programStageDataElement(programStage, dataElementsByUid.getValue(dataElementUid))
            }

        whenever(d2.programModule().programStageDataElements()) doReturn programStageDataElements
        whenever(programStageDataElements.withRenderType()) doReturn programStageDataElementsWithRenderType
        whenever(programStageDataElementsWithRenderType.byProgramStage()) doReturn programStageDataElementProgramStageFilter
        whenever(programStageDataElementProgramStageFilter.eq(PROGRAM_STAGE_UID)) doReturn programStageDataElementsByStage
        whenever(
            programStageDataElementsByStage.orderBySortOrder(RepositoryScope.OrderByDirection.ASC),
        ) doReturn sortedProgramStageDataElements
        whenever(sortedProgramStageDataElements.blockingGet()) doReturn programStageDataElementsForStage
        whenever(d2.programModule().programStageSections()) doReturn programStageSections
        whenever(programStageSections.byProgramStageUid()) doReturn programStageSectionFilter
        whenever(programStageSectionFilter.eq(PROGRAM_STAGE_UID)) doReturn programStageSectionsByStage
        whenever(programStageSectionsByStage.withDataElements()) doReturn programStageSectionsWithDataElements
        whenever(programStageSectionsWithDataElements.blockingGet()) doReturn
            sections.map { section ->
                ProgramStageSection
                    .builder()
                    .uid(section.uid)
                    .displayName(section.displayName)
                    .sortOrder(section.sortOrder)
                    .programStage(programStage)
                    .dataElements(section.dataElementUids.map(dataElementsByUid::getValue))
                    .build()
            }
        whenever(
            d2
                .dataElementModule()
                .dataElements()
                .uid(WEIGHT_UID)
                .blockingGet(),
        ) doReturn
            DataElement
                .builder()
                .uid(WEIGHT_UID)
                .displayShortName("Weight")
                .displayName("Weight (kg)")
                .valueType(ValueType.NUMBER)
                .build()
        whenever(
            d2
                .dataElementModule()
                .dataElements()
                .uid(STATUS_UID)
                .blockingGet(),
        ) doReturn
            DataElement
                .builder()
                .uid(STATUS_UID)
                .displayShortName("Status")
                .valueType(ValueType.TEXT)
                .optionSet(ObjectWithUid.create(OPTION_SET_UID))
                .build()
        whenever(
            d2
                .dataElementModule()
                .dataElements()
                .uid(BOOLEAN_UID)
                .blockingGet(),
        ) doReturn
            DataElement
                .builder()
                .uid(BOOLEAN_UID)
                .displayShortName("Confirmed")
                .valueType(ValueType.BOOLEAN)
                .build()
        whenever(
            d2
                .dataElementModule()
                .dataElements()
                .uid(EXCLUDED_UID)
                .blockingGet(),
        ) doReturn
            DataElement
                .builder()
                .uid(EXCLUDED_UID)
                .displayShortName("Excluded")
                .valueType(ValueType.TEXT)
                .build()
    }

    private fun stubOptions() {
        val statusOptions =
            listOf(
                Option
                    .builder()
                    .uid("option-a")
                    .code("A")
                    .displayName("OptionA")
                    .build(),
                Option
                    .builder()
                    .uid("option-b")
                    .code("B")
                    .displayName("OptionB")
                    .build(),
            )

        whenever(d2.optionModule().options()) doReturn options
        whenever(options.byOptionSetUid()) doReturn optionSetFilter
        whenever(optionSetFilter.eq(OPTION_SET_UID)) doReturn optionsBySet
        whenever(optionsBySet.blockingGet()) doReturn statusOptions
    }

    private fun programStageDataElement(
        programStage: ObjectWithUid,
        dataElement: DataElement,
    ): ProgramStageDataElement =
        mock {
            on { programStage() } doReturn programStage
            on { dataElement() } doReturn dataElement
        }

    private fun event(
        uid: String,
        eventDate: Date,
        values: List<TrackedEntityDataValue>,
    ): Event =
        Event
            .builder()
            .uid(uid)
            .program(PROGRAM_UID)
            .programStage(PROGRAM_STAGE_UID)
            .enrollment(ENROLLMENT_UID)
            .eventDate(eventDate)
            .trackedEntityDataValues(values)
            .build()

    private fun value(
        dataElementUid: String,
        value: String,
    ): TrackedEntityDataValue =
        TrackedEntityDataValue
            .builder()
            .dataElement(dataElementUid)
            .value(value)
            .build()

    private fun dataElement(uid: String): DataElement = DataElement.builder().uid(uid).build()

    private data class TestSectionDefinition(
        val uid: String,
        val displayName: String,
        val sortOrder: Int?,
        val dataElementUids: List<String>,
    )

    private companion object {
        const val CURRENT_EVENT_UID = "current"
        const val ENROLLMENT_UID = "enrollment"
        const val PROGRAM_UID = "program"
        const val PROGRAM_STAGE_UID = "follow-stage"
        const val VISIT_NUMBER_UID = "visit-number"
        const val WEIGHT_UID = "weight"
        const val STATUS_UID = "status"
        const val BOOLEAN_UID = "boolean"
        const val EXCLUDED_UID = "excluded"
        const val OPTION_SET_UID = "option-set"
    }
}
