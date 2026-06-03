package org.dhis2.simprints.ramp.data

import org.dhis2.bindings.userFriendlyValue
import org.dhis2.commons.simprints.ramp.model.ProgramStageHistoryTableConfig
import org.dhis2.commons.simprints.ramp.repository.RampDatastoreRepository
import org.dhis2.simprints.ramp.model.EventHistoryTable
import org.dhis2.simprints.ramp.model.EventHistoryTableCell
import org.dhis2.simprints.ramp.model.EventHistoryTableColumn
import org.dhis2.simprints.ramp.model.EventHistoryTableRow
import org.dhis2.simprints.ramp.model.EventHistoryTableSection
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
    private val simprintsRampDatastoreRepository: RampDatastoreRepository,
    private val eventUid: String? = null,
    private val programUid: String? = null,
    private val enrollmentUid: String? = null,
) {
    fun getTable(): EventHistoryTable? {
        val tableContext = getTableContext() ?: return null
        val config = tableContext.config
        val followUpVisitProgramStageUid = config.followUpVisitProgramStageId.trimToValue() ?: return null
        val followUpVisitMaxNumber = config.followUpVisitMaxNumber?.takeIf { it >= 0 } ?: return null
        val headerVisitNumberDataElementUid = config.headerVisitNumberDataElementId.trimToValue() ?: return null
        val excludedDataElementIds =
            (
                config.excludedFollowUpVisitDataElementIds
                    .orEmpty()
                    .mapNotNull { dataElementId -> dataElementId.trimToValue() } +
                    headerVisitNumberDataElementUid
            ).toSet()
        val rowDefinitions = getRowDefinitions(followUpVisitProgramStageUid, excludedDataElementIds)
        if (rowDefinitions.isEmpty()) {
            return null
        }

        val events = getHistoryEvents(tableContext, followUpVisitProgramStageUid)
        val eventDataValuesByUid = events.associate { event -> event.uid() to event.dataValuesByDataElement() }
        val eventsByColumnIndex =
            getEventsByVisitNumberColumn(
                events = events,
                eventDataValuesByUid = eventDataValuesByUid,
                headerVisitNumberDataElementUid = headerVisitNumberDataElementUid,
                followUpVisitMaxNumber = followUpVisitMaxNumber,
            )
        val columnIndexes = (0..followUpVisitMaxNumber).toList()
        val optionDisplayNamesBySet = getOptionDisplayNamesBySet(rowDefinitions)

        fun eventForColumn(columnIndex: Int): Event? = eventsByColumnIndex[columnIndex]

        val tableColumns =
            columnIndexes.map { columnIndex ->
                EventHistoryTableColumn(
                    eventUid = eventForColumn(columnIndex)?.uid(),
                    label = columnIndex.toString(),
                )
            }
        val dateRowValues =
            columnIndexes.map { columnIndex ->
                eventForColumn(columnIndex).displayDate().toHistoryTableDateLabel()
            }
        val sections =
            rowDefinitions.mapNotNull { section ->
                section
                    .rows
                    .map { row ->
                        EventHistoryTableRow(
                            label = row.label,
                            values =
                                columnIndexes.map { columnIndex ->
                                    val event = eventForColumn(columnIndex)
                                    EventHistoryTableCell(
                                        value =
                                            row.displayValue(
                                                rawValue =
                                                    event
                                                        ?.let { eventDataValuesByUid[it.uid()] }
                                                        ?.get(row.dataElementUid)
                                                        .orEmpty(),
                                                optionDisplayNamesBySet = optionDisplayNamesBySet,
                                            ),
                                        valueType = row.valueType,
                                    )
                                },
                        )
                    }.takeIf { it.isNotEmpty() }
                    ?.let { rows ->
                        EventHistoryTableSection(
                            title = section.title,
                            rows = rows,
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

    private fun getTableContext(): HistoryTableContext? {
        val currentEvent =
            eventUid
                .trimToValue()
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
                getProgramStageHistoryTableConfig(
                    programId = programUid,
                    programStageId = programStageUid,
                ) ?: return null

            return HistoryTableContext(
                config = config,
                enrollmentUid = currentEvent.enrollment(),
                currentEvent = currentEvent,
            )
        }

        val programUid = programUid.trimToValue() ?: return null
        val enrollmentUid = enrollmentUid.trimToValue() ?: return null
        val config =
            getProgramStageHistoryTableConfig(
                programId = programUid,
                programStageId = null,
            ) ?: return null

        return HistoryTableContext(
            config = config,
            enrollmentUid = enrollmentUid,
            currentEvent = null,
        )
    }

    private fun getProgramStageHistoryTableConfig(
        programId: String,
        programStageId: String?,
    ): ProgramStageHistoryTableConfig? =
        simprintsRampDatastoreRepository
            .getConfig()
            .programStageHistoryTables
            .firstOrNull { config ->
                config.programId.trimToValue() == programId &&
                    (
                        programStageId == null ||
                            config.followUpVisitProgramStageId.trimToValue() == programStageId
                    )
            }

    private fun getRowDefinitions(
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
                .sortedWith(compareBy({ section -> section.sortOrder() ?: Int.MAX_VALUE }, { section -> section.uid() }))

        return if (sections.isEmpty()) {
            getSingleSectionRowDefinitions(
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
                val rows = followUpRows.filter { row -> row.dataElementUid in sectionDataElementUids }

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

    private fun getSingleSectionRowDefinitions(
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

    private fun ProgramStageDataElement.toRowDefinition(excludedDataElementIds: Set<String>): HistoryTableRowDefinition? {
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

    private fun getHistoryEvents(
        tableContext: HistoryTableContext,
        followUpVisitProgramStageUid: String,
    ): List<Event> {
        val currentEventDate = tableContext.currentEvent?.displayDate()
        val queriedEvents =
            if (tableContext.enrollmentUid.isNullOrBlank()) {
                emptyList()
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
        val events =
            (queriedEvents + listOfNotNull(tableContext.currentEvent))
                .associateBy { event -> event.uid() }
                .values

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

    private fun getEventsByVisitNumberColumn(
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

    private fun getOptionDisplayNamesBySet(rowDefinitions: List<HistoryTableSectionDefinition>): Map<String, Map<String, String>> =
        rowDefinitions
            .flatMap { it.rows }
            .mapNotNull { row ->
                row.optionSetUid?.takeIf { row.valueType != ValueType.MULTI_TEXT }
            }.distinct()
            .associateWith(::getOptionDisplayNames)

    private fun getOptionDisplayNames(optionSetUid: String): Map<String, String> =
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
        this
            ?.let { SimpleDateFormat(HISTORY_TABLE_DATE_LABEL_FORMAT, Locale.getDefault()).format(it) }
            .orEmpty()

    private fun String?.trimToValue(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

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

    private companion object {
        private const val HISTORY_TABLE_DATE_LABEL_FORMAT = "MMM d"
    }
}
