package org.dhis2.form.simprints.ramp.data

import org.dhis2.commons.simprints.ramp.model.DataElementHistoryChartConfig
import org.dhis2.form.model.FieldUiModelImpl
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.filters.internal.BooleanFilterConnector
import org.hisp.dhis.android.core.arch.repositories.filters.internal.StringFilterConnector
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.EventCollectionRepository
import org.hisp.dhis.android.core.event.EventObjectRepository
import org.hisp.dhis.android.core.option.Option
import org.hisp.dhis.android.core.option.OptionCollectionRepository
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

class FormHistoryChartRepositoryTest {
    private val d2: D2 = Mockito.mock(D2::class.java, Mockito.RETURNS_DEEP_STUBS)
    private val events: EventCollectionRepository = mock()
    private val currentEventRepository: EventObjectRepository = mock()
    private val eventsWithDataValues: EventCollectionRepository = mock()
    private val enrollmentFilter: StringFilterConnector<EventCollectionRepository> = mock()
    private val enrollmentEvents: EventCollectionRepository = mock()
    private val programStageFilter: StringFilterConnector<EventCollectionRepository> = mock()
    private val followUpEvents: EventCollectionRepository = mock()
    private val deletedFilter: BooleanFilterConnector<EventCollectionRepository> = mock()
    private val activeFollowUpEvents: EventCollectionRepository = mock()
    private val optionCollection: OptionCollectionRepository = mock()
    private val optionSetFilter: StringFilterConnector<OptionCollectionRepository> = mock()
    private val optionsByOptionSet: OptionCollectionRepository = mock()
    private val orderedOptions: OptionCollectionRepository = mock()
    private val repository = FormHistoryChartRepository(CURRENT_EVENT_UID, d2)

    @Test
    fun `getChart should return visit-number chart with current input value and no future values`() {
        val currentEvent =
            getEvent(
                uid = CURRENT_EVENT_UID,
                eventDate = Date(2_000), // millis
                dataValues =
                    listOf(
                        dataValue(VISIT_NUMBER_UID, "1"),
                        dataValue(DATA_ELEMENT_UID, "9.0"),
                    ),
            )
        stubCurrentEvent(currentEvent)
        stubFollowUpEvents(
            listOf(
                getEvent(
                    uid = "previous",
                    eventDate = Date(1_000),
                    dataValues =
                        listOf(
                            dataValue(VISIT_NUMBER_UID, "0"),
                            dataValue(DATA_ELEMENT_UID, "8.0"),
                        ),
                ),
                currentEvent,
                getEvent(
                    uid = "future",
                    eventDate = Date(3_000),
                    dataValues =
                        listOf(
                            dataValue(VISIT_NUMBER_UID, "2"),
                            dataValue(DATA_ELEMENT_UID, "10.0"),
                        ),
                ),
            ),
        )

        val chart =
            repository.getChart(
                fieldUiModel =
                    FieldUiModelImpl(
                        uid = DATA_ELEMENT_UID,
                        value = "9.5",
                        label = "Weight",
                        valueType = ValueType.NUMBER,
                        optionSetConfiguration = null,
                        autocompleteList = null,
                    ),
                configs = listOf(getConfig(isYAxisInverted = true)),
            )

        assertEquals("Weight", chart?.title)
        assertEquals(listOf("0", "1", "2", "3"), chart?.labels)
        assertEquals(listOf(8f, 9.5f, null, null), chart?.values)
        assertEquals(1, chart?.currentValueIndex)
        assertEquals(true, chart?.isYAxisInverted)
        assertEquals(1, chart?.displayMaxDecimalPlaces)
    }

    @Test
    fun `getChart should plot current input value when current event not saved yet`() {
        val currentEvent =
            getEvent(
                uid = CURRENT_EVENT_UID,
                eventDate = Date(2_000),
                dataValues =
                    listOf(
                        dataValue(VISIT_NUMBER_UID, "1"),
                        dataValue(DATA_ELEMENT_UID, "9.0"),
                    ),
            )
        stubCurrentEvent(currentEvent)
        stubFollowUpEvents(
            listOf(
                getEvent(
                    uid = "previous",
                    eventDate = Date(1_000),
                    dataValues =
                        listOf(
                            dataValue(VISIT_NUMBER_UID, "0"),
                            dataValue(DATA_ELEMENT_UID, "8.0"),
                        ),
                ),
            ),
        )

        val chart =
            repository.getChart(
                fieldUiModel =
                    FieldUiModelImpl(
                        uid = DATA_ELEMENT_UID,
                        value = "9.5",
                        label = "Weight",
                        valueType = ValueType.NUMBER,
                        optionSetConfiguration = null,
                        autocompleteList = null,
                    ),
                configs = listOf(getConfig()),
            )

        assertEquals(listOf(8f, 9.5f, null, null), chart?.values)
        assertEquals(1, chart?.currentValueIndex)
    }

    @Test
    fun `getChart should plot option set values in metadata order`() {
        val currentEvent =
            getEvent(
                uid = CURRENT_EVENT_UID,
                eventDate = Date(2_000),
                dataValues =
                    listOf(
                        dataValue(VISIT_NUMBER_UID, "1"),
                        dataValue(DATA_ELEMENT_UID, "mam"),
                    ),
            )
        stubCurrentEvent(currentEvent)
        stubFollowUpEvents(
            listOf(
                getEvent(
                    uid = "previous",
                    eventDate = Date(1_000),
                    dataValues =
                        listOf(
                            dataValue(VISIT_NUMBER_UID, "0"),
                            dataValue(DATA_ELEMENT_UID, "sam"),
                        ),
                ),
                currentEvent,
            ),
        )
        stubOptions(
            option(code = "ok", displayName = "Ok", sortOrder = 0),
            option(code = "mam", displayName = "MAM", sortOrder = 1),
            option(code = "sam", displayName = "SAM", sortOrder = 2),
        )

        val chart =
            repository.getChart(
                fieldUiModel =
                    FieldUiModelImpl(
                        uid = DATA_ELEMENT_UID,
                        value = "Ok",
                        label = "Nutrition Status",
                        valueType = ValueType.TEXT,
                        optionSet = OPTION_SET_UID,
                        optionSetConfiguration = null,
                        autocompleteList = null,
                    ),
                configs = listOf(getConfig()),
            )

        assertEquals(listOf("Ok", "MAM", "SAM"), chart?.categories?.map { it.displayName })
        assertEquals(listOf(2f, 0f, null, null), chart?.values)
        assertEquals(listOf(2f, 1f, null, null), chart?.withCurrentValue("mam")?.values)
        assertEquals(listOf(2f, 2f, null, null), chart?.withCurrentValue("SAM")?.values)
        assertEquals(listOf(2f, null, null, null), chart?.withCurrentValue(null)?.values)
        assertEquals(listOf(2f, null, null, null), chart?.withCurrentValue("unknown")?.values)
    }

    @Test
    fun `getChart should return null when field is not configured`() {
        stubCurrentEvent(getEvent(uid = CURRENT_EVENT_UID))

        val chart =
            repository.getChart(
                fieldUiModel =
                    FieldUiModelImpl(
                        uid = "unconfigured-data-element",
                        label = "Height",
                        valueType = ValueType.NUMBER,
                        optionSetConfiguration = null,
                        autocompleteList = null,
                    ),
                configs = listOf(getConfig()),
            )

        assertNull(chart)
        verify(currentEventRepository, times(0)).blockingGet()
    }

    @Test
    fun `getChart should not fall back to stored current event value when current input is empty`() {
        val currentEvent =
            getEvent(
                uid = CURRENT_EVENT_UID,
                dataValues =
                    listOf(
                        dataValue(VISIT_NUMBER_UID, "1"),
                        dataValue(DATA_ELEMENT_UID, "9.0"),
                    ),
            )
        stubCurrentEvent(currentEvent)
        stubFollowUpEvents(listOf(currentEvent))

        val chart =
            repository.getChart(
                fieldUiModel =
                    FieldUiModelImpl(
                        uid = DATA_ELEMENT_UID,
                        value = "",
                        label = "Weight",
                        valueType = ValueType.NUMBER,
                        optionSetConfiguration = null,
                        autocompleteList = null,
                    ),
                configs = listOf(getConfig()),
            )

        assertEquals(listOf(null, null, null, null), chart?.values)
        assertEquals(1, chart?.currentValueIndex)
    }

    @Test
    fun `getChart should exclude locally deleted follow-up events`() {
        val currentEvent =
            getEvent(
                uid = CURRENT_EVENT_UID,
                eventDate = Date(2_000),
                dataValues =
                    listOf(
                        dataValue(VISIT_NUMBER_UID, "1"),
                        dataValue(DATA_ELEMENT_UID, "9.0"),
                    ),
            )
        stubCurrentEvent(currentEvent)
        stubFollowUpEvents(
            eventsToReturn =
                listOf(
                    getEvent(
                        uid = "active",
                        eventDate = Date(1_000),
                        dataValues =
                            listOf(
                                dataValue(VISIT_NUMBER_UID, "0"),
                                dataValue(DATA_ELEMENT_UID, "8.0"),
                            ),
                    ),
                ),
            unfilteredEvents =
                listOf(
                    getEvent(
                        uid = "deleted",
                        eventDate = Date(1_000),
                        dataValues =
                            listOf(
                                dataValue(VISIT_NUMBER_UID, "0"),
                                dataValue(DATA_ELEMENT_UID, "99.0"),
                            ),
                    ).toBuilder().deleted(true).build(),
                ),
        )

        val chart =
            repository.getChart(
                fieldUiModel =
                    FieldUiModelImpl(
                        uid = DATA_ELEMENT_UID,
                        value = "9.5",
                        label = "Weight",
                        valueType = ValueType.NUMBER,
                        optionSetConfiguration = null,
                        autocompleteList = null,
                    ),
                configs = listOf(getConfig()),
            )

        assertEquals(listOf(8f, 9.5f, null, null), chart?.values)
    }

    @Test
    fun `getChart should reuse loaded current event and follow up events`() {
        val currentEvent =
            getEvent(
                uid = CURRENT_EVENT_UID,
                dataValues =
                    listOf(
                        dataValue(VISIT_NUMBER_UID, "1"),
                        dataValue(DATA_ELEMENT_UID, "9.0"),
                    ),
            )
        val fieldUiModel =
            FieldUiModelImpl(
                uid = DATA_ELEMENT_UID,
                value = "9.5",
                label = "Weight",
                valueType = ValueType.NUMBER,
                optionSetConfiguration = null,
                autocompleteList = null,
            )
        stubCurrentEvent(currentEvent)
        stubFollowUpEvents(listOf(currentEvent))

        repository.getChart(fieldUiModel, listOf(getConfig()))
        repository.getChart(fieldUiModel, listOf(getConfig()))

        verify(currentEventRepository, times(2)).blockingGet()
        verify(activeFollowUpEvents, times(1)).blockingGet()
    }

    private fun getConfig(isYAxisInverted: Boolean = false) =
        DataElementHistoryChartConfig(
            programId = PROGRAM_UID,
            followUpVisitProgramStageId = PROGRAM_STAGE_UID,
            dataElementId = DATA_ELEMENT_UID,
            xAxisVisitNumberDataElementId = VISIT_NUMBER_UID,
            followUpVisitMaxNumber = 3,
            isYAxisInverted = isYAxisInverted,
            displayMaxDecimalPlaces = 1,
        )

    private fun stubCurrentEvent(event: Event) {
        whenever(d2.eventModule().events()) doReturn events
        whenever(events.withTrackedEntityDataValues()) doReturn eventsWithDataValues
        whenever(eventsWithDataValues.uid(CURRENT_EVENT_UID)) doReturn currentEventRepository
        whenever(currentEventRepository.blockingGet()) doReturn event
    }

    private fun stubFollowUpEvents(
        eventsToReturn: List<Event>,
        unfilteredEvents: List<Event> = eventsToReturn,
    ) {
        whenever(events.withTrackedEntityDataValues()) doReturn eventsWithDataValues
        whenever(eventsWithDataValues.byEnrollmentUid()) doReturn enrollmentFilter
        whenever(enrollmentFilter.eq(ENROLLMENT_UID)) doReturn enrollmentEvents
        whenever(enrollmentEvents.byProgramStageUid()) doReturn programStageFilter
        whenever(programStageFilter.eq(PROGRAM_STAGE_UID)) doReturn followUpEvents
        whenever(followUpEvents.blockingGet()) doReturn unfilteredEvents
        whenever(followUpEvents.byDeleted()) doReturn deletedFilter
        whenever(deletedFilter.isFalse) doReturn activeFollowUpEvents
        whenever(activeFollowUpEvents.blockingGet()) doReturn eventsToReturn
    }

    private fun stubOptions(vararg options: Option) {
        whenever(d2.optionModule().options()) doReturn optionCollection
        whenever(optionCollection.byOptionSetUid()) doReturn optionSetFilter
        whenever(optionSetFilter.eq(OPTION_SET_UID)) doReturn optionsByOptionSet
        whenever(
            optionsByOptionSet.orderBySortOrder(RepositoryScope.OrderByDirection.ASC),
        ) doReturn orderedOptions
        whenever(orderedOptions.blockingGet()) doReturn options.toList()
    }

    private fun option(
        code: String,
        displayName: String,
        sortOrder: Int,
    ): Option =
        Option
            .builder()
            .uid("$code-option")
            .code(code)
            .name(displayName)
            .displayName(displayName)
            .sortOrder(sortOrder)
            .build()

    private fun getEvent(
        uid: String,
        eventDate: Date = Date(1_000),
        dataValues: List<TrackedEntityDataValue> = emptyList(),
    ): Event =
        Event
            .builder()
            .uid(uid)
            .program(PROGRAM_UID)
            .programStage(PROGRAM_STAGE_UID)
            .enrollment(ENROLLMENT_UID)
            .eventDate(eventDate)
            .trackedEntityDataValues(dataValues)
            .build()

    private fun dataValue(
        dataElementUid: String,
        value: String,
    ): TrackedEntityDataValue =
        TrackedEntityDataValue
            .builder()
            .event(CURRENT_EVENT_UID)
            .dataElement(dataElementUid)
            .value(value)
            .build()

    private companion object {
        const val CURRENT_EVENT_UID = "current-event"
        const val ENROLLMENT_UID = "enrollment"
        const val PROGRAM_UID = "program"
        const val PROGRAM_STAGE_UID = "follow-stage"
        const val DATA_ELEMENT_UID = "weight"
        const val VISIT_NUMBER_UID = "visit-number"
        const val OPTION_SET_UID = "nutrition-status-options"
    }
}
