package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.history

import org.dhis2.bindings.userFriendlyValue
import org.dhis2.commons.simprints.RampDatastoreConfig
import org.dhis2.commons.simprints.ProgramStageHistoryTableConfig
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.program.ProgramStageDataElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventHistoryTableRepository(
    private val d2: D2,
    private val eventUid: String? = null,
    private val programUid: String? = null,
    private val enrollmentUid: String? = null,
) {
    fun table(): EventHistoryTable? {
        val tableContext = tableContext() ?: return null
        val config = tableContext.config
        val followUpVisitProgramStageUid =
            config.followUpVisitProgramStageId
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return null
        val followUpVisitMaxNumber =
            config.followUpVisitMaxNumber
                ?.takeIf { it >= 0 }
                ?: return null
        val headerVisitNumberDataElementUid =
            config.headerVisitNumberDataElementId
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return null
        val excludedDataElementIds =
            (
                config.excludedFollowUpVisitDataElementIds.orEmpty()
                    .mapNotNull { it.trim().takeIf(String::isNotEmpty) } +
                    headerVisitNumberDataElementUid
            ).toSet()
        val rowDefinitions = rowDefinitions(followUpVisitProgramStageUid, excludedDataElementIds)
        if (rowDefinitions.isEmpty()) {
            return null
        }

        val events = historyEvents(tableContext, followUpVisitProgramStageUid)
        val eventDataValuesByUid =
            events.associate { event -> event.uid() to event.dataValuesByDataElement() }
        val eventsByColumnIndex =
            eventsByVisitNumberColumn(
                events = events,
                eventDataValuesByUid = eventDataValuesByUid,
                headerVisitNumberDataElementUid = headerVisitNumberDataElementUid,
                followUpVisitMaxNumber = followUpVisitMaxNumber,
            )
        val columnIndexes = (0..followUpVisitMaxNumber).toList()
        val optionDisplayNamesBySet = optionDisplayNamesBySet(rowDefinitions)
        fun eventForColumn(columnIndex: Int): Event? = eventsByColumnIndex[columnIndex]

        val tableColumns =
            columnIndexes.map { columnIndex ->
                val event = eventForColumn(columnIndex)
                EventHistoryTableColumn(
                    eventUid = event?.uid(),
                    label = columnIndex.toString(),
                )
            }
        val dateRowValues =
            columnIndexes.map { columnIndex ->
                eventForColumn(columnIndex).displayDate().toHistoryTableDateLabel()
            }

        val sections =
            rowDefinitions.mapNotNull { section ->
                val rows =
                    section.rows.map { row ->
                        EventHistoryTableRow(
                            label = row.label,
                            values =
                                columnIndexes.map { columnIndex ->
                                    val event = eventForColumn(columnIndex)
                                    row.displayValue(
                                        rawValue = event
                                            ?.let { eventDataValuesByUid[it.uid()] }
                                            ?.get(row.dataElementUid)
                                            .orEmpty(),
                                        optionDisplayNamesBySet = optionDisplayNamesBySet,
                                    )
                                },
                        )
                    }

                rows
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        EventHistoryTableSection(
                            title = section.title,
                            rows = it,
                        )
                    }
            }

        return sections
            .takeIf { it.isNotEmpty() }
            ?.let {
                EventHistoryTable(
                    columns = tableColumns,
                    sections = it,
                    dateRowValues = dateRowValues,
                )
            }
    }

    private fun tableContext(): HistoryTableContext? {
        val currentEvent =
            eventUid
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { eventUid ->
                    d2
                        .eventModule()
                        .events()
                        .withTrackedEntityDataValues()
                        .byUid()
                        .eq(eventUid)
                        .one()
                        .blockingGet()
                }

        if (currentEvent != null) {
            val programUid = currentEvent.program() ?: return null
            val programStageUid = currentEvent.programStage() ?: return null
            val config =
                RampDatastoreConfig.localProgramStageHistoryTableConfig(
                    d2 = d2,
                    programId = programUid,
                    programStageId = programStageUid,
                ) ?: return null

            return HistoryTableContext(
                config = config,
                enrollmentUid = currentEvent.enrollment(),
                currentEvent = currentEvent,
            )
        }

        val programUid = programUid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val enrollmentUid = enrollmentUid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val config =
            RampDatastoreConfig.localProgramStageHistoryTableConfig(
                d2 = d2,
                programId = programUid,
            ) ?: return null

        return HistoryTableContext(
            config = config,
            enrollmentUid = enrollmentUid,
            currentEvent = null,
        )
    }

    private fun rowDefinitions(
        programStageUid: String,
        excludedDataElementIds: Set<String>,
    ): List<HistoryTableSectionDefinition> {
        val followUpRows =
            d2
                .programModule()
                .programStageDataElements()
                .withRenderType()
                .byProgramStage()
                .eq(programStageUid)
                .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
                .blockingGet()
                .asSequence()
                .filter { programStageDataElement ->
                    programStageDataElement.programStage()?.uid() == programStageUid
                }.mapNotNull { programStageDataElement ->
                    programStageDataElement.toRowDefinition(excludedDataElementIds)
                }.distinctBy { row -> row.dataElementUid }
                .toList()
        val sections =
            d2
                .programModule()
                .programStageSections()
                .byProgramStageUid()
                .eq(programStageUid)
                .withDataElements()
                .blockingGet()
                .filter { section -> section.programStage()?.uid() == programStageUid }
                .sortedWith(compareBy({ it.sortOrder() ?: Int.MAX_VALUE }, { it.uid() }))

        return if (sections.isEmpty()) {
            singleSectionRowDefinitions(
                programStageUid = programStageUid,
                rows = followUpRows,
            )
        } else {
            sections.mapNotNull { section ->
                val sectionDataElementUids =
                    section
                        .dataElements()
                        .orEmpty()
                        .mapNotNull { dataElement -> dataElement.uid() }
                        .toSet()
                val rows =
                    followUpRows.filter { row -> row.dataElementUid in sectionDataElementUids }

                rows
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        HistoryTableSectionDefinition(
                            title = section.displayName() ?: section.uid(),
                            rows = it,
                        )
                    }
            }
        }
    }

    private fun singleSectionRowDefinitions(
        programStageUid: String,
        rows: List<HistoryTableRowDefinition>,
    ): List<HistoryTableSectionDefinition> {
        val programStage =
            d2
                .programModule()
                .programStages()
                .uid(programStageUid)
                .blockingGet()

        return rows
            .takeIf { it.isNotEmpty() }
            ?.let {
                listOf(
                    HistoryTableSectionDefinition(
                        title = programStage?.displayName() ?: programStageUid,
                        rows = it,
                    ),
                )
            }.orEmpty()
    }

    private fun ProgramStageDataElement.toRowDefinition(
        excludedDataElementIds: Set<String>,
    ): HistoryTableRowDefinition? {
        val uid = dataElement()?.uid() ?: return null
        if (uid in excludedDataElementIds) {
            return null
        }
        val dataElement =
            d2
                .dataElementModule()
                .dataElements()
                .uid(uid)
                .blockingGet() ?: return null
        return HistoryTableRowDefinition(
            dataElementUid = uid,
            label =
                dataElement.displayShortName()
                    ?: dataElement.shortName()
                    ?: dataElement.displayFormName()
                    ?: dataElement.displayName()
                    ?: uid,
            valueType = dataElement.valueType(),
            optionSetUid = dataElement.optionSetUid(),
        )
    }

    private fun historyEvents(
        tableContext: HistoryTableContext,
        followUpVisitProgramStageUid: String,
    ): List<Event> {
        val currentEventDate = tableContext.currentEvent?.displayDate()
        val events =
            if (tableContext.enrollmentUid.isNullOrBlank()) {
                listOfNotNull(tableContext.currentEvent)
            } else {
                d2
                    .eventModule()
                    .events()
                    .withTrackedEntityDataValues()
                    .byEnrollmentUid()
                    .eq(tableContext.enrollmentUid)
                    .byProgramStageUid()
                    .eq(followUpVisitProgramStageUid)
                    .blockingGet()
            }

        return events
            .filter { event ->
                val eventDate = event.displayDate()
                event.uid() == tableContext.currentEvent?.uid() ||
                    currentEventDate == null ||
                    eventDate == null ||
                    !eventDate.after(currentEventDate)
            }.sortedWith(
                compareBy(
                    { event -> event.displayDate() ?: Date(0) },
                    { event -> event.uid() },
                ),
            )
    }

    private fun eventsByVisitNumberColumn(
        events: List<Event>,
        eventDataValuesByUid: Map<String, Map<String, String>>,
        headerVisitNumberDataElementUid: String,
        followUpVisitMaxNumber: Int,
    ): Map<Int, Event> =
        events
            .mapNotNull { event ->
                eventDataValuesByUid[event.uid()]
                    ?.get(headerVisitNumberDataElementUid)
                    ?.toVisitNumber()
                    ?.takeIf { it in 0..followUpVisitMaxNumber }
                    ?.let { visitNumber -> visitNumber to event }
            }.toMap()

    private fun Event.dataValuesByDataElement(): Map<String, String> =
        trackedEntityDataValues()
            .orEmpty()
            .mapNotNull { dataValue ->
                val dataElementUid = dataValue.dataElement()
                val value = dataValue.value()
                if (dataElementUid == null || value == null) {
                    null
                } else {
                    dataElementUid to value
                }
            }.toMap()

    private fun optionDisplayNamesBySet(
        rowDefinitions: List<HistoryTableSectionDefinition>,
    ): Map<String, Map<String, String>> =
        rowDefinitions
            .flatMap { it.rows }
            .mapNotNull { row ->
                row.optionSetUid?.takeIf { row.valueType != ValueType.MULTI_TEXT }
            }.distinct()
            .associateWith(::optionDisplayNames)

    private fun optionDisplayNames(optionSetUid: String): Map<String, String> =
        d2
            .optionModule()
            .options()
            .byOptionSetUid()
            .eq(optionSetUid)
            .blockingGet()
            .flatMap { option ->
                val displayName = option.displayName() ?: option.name() ?: option.code().orEmpty()
                listOfNotNull(
                    option.code()?.let { it to displayName },
                    option.displayName()?.let { it to displayName },
                    option.name()?.let { it to displayName },
                )
            }.toMap()

    private fun HistoryTableRowDefinition.displayValue(
        rawValue: String,
        optionDisplayNamesBySet: Map<String, Map<String, String>>,
    ): String {
        if (rawValue.isEmpty()) {
            return rawValue
        }

        optionSetUid?.takeIf { valueType != ValueType.MULTI_TEXT }?.let { optionSetUid ->
            return optionDisplayNamesBySet[optionSetUid]?.get(rawValue) ?: rawValue
        }

        return rawValue.userFriendlyValue(
            d2 = d2,
            valueType = valueType,
            optionSetUid = optionSetUid,
            addPercentageSymbol = false,
        ) ?: rawValue
    }

    private fun Event?.displayDate(): Date? = this?.eventDate() ?: this?.dueDate() ?: this?.created()

    private fun Date?.toHistoryTableDateLabel(): String =
        this?.let {
            SimpleDateFormat(HISTORY_TABLE_DATE_LABEL_FORMAT, Locale.getDefault()).format(it)
        }.orEmpty()

    private fun String.toVisitNumber(): Int? {
        val number = trim().toDoubleOrNull() ?: return null
        val integer = number.toInt()
        return integer.takeIf { it.toDouble() == number }
    }

    private data class HistoryTableSectionDefinition(
        val title: String,
        val rows: List<HistoryTableRowDefinition>,
    )

    private data class HistoryTableRowDefinition(
        val dataElementUid: String,
        val label: String,
        val valueType: ValueType?,
        val optionSetUid: String?,
    )

    private data class HistoryTableContext(
        val config: ProgramStageHistoryTableConfig,
        val enrollmentUid: String?,
        val currentEvent: Event?,
    )

    companion object {
        private const val HISTORY_TABLE_DATE_LABEL_FORMAT = "MMM d"
    }
}
