package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.history

import org.dhis2.bindings.userFriendlyValue
import org.dhis2.commons.simprints.RampDatastoreConfig
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.program.ProgramStageDataElement
import org.hisp.dhis.android.core.program.ProgramStageSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventHistoryTableRepository(
    private val d2: D2,
    private val eventUid: String,
) {
    fun table(): EventHistoryTable? {
        val currentEvent =
            d2
                .eventModule()
                .events()
                .withTrackedEntityDataValues()
                .byUid()
                .eq(eventUid)
                .one()
                .blockingGet() ?: return null
        val programUid = currentEvent.program() ?: return null
        val programStageUid = currentEvent.programStage() ?: return null
        val config =
            RampDatastoreConfig.localProgramStageHistoryTableConfig(
                d2 = d2,
                programId = programUid,
                programStageId = programStageUid,
            ) ?: return null
        val columns = config.dataPointColumnsInTable?.coerceAtLeast(1) ?: DEFAULT_COLUMN_COUNT
        val excludedDataElementIds = config.excludedDataElementIds.orEmpty().toSet()
        val rowDefinitions = rowDefinitions(programStageUid, excludedDataElementIds)
        if (rowDefinitions.isEmpty()) {
            return null
        }

        val events = historyEvents(currentEvent, columns)
        val eventDataValuesByUid = events.associate { event -> event.uid() to event.dataValuesByDataElement() }
        val optionDisplayNamesBySet = optionDisplayNamesBySet(rowDefinitions)
        val tableColumns =
            (0 until columns).map { columnIndex ->
                val event = events.getOrNull(columnIndex)
                EventHistoryTableColumn(
                    eventUid = event?.uid(),
                    label = event.displayDate().toHistoryTableDateLabel(),
                )
            }

        val sections =
            rowDefinitions.mapNotNull { section ->
                val rows =
                    section.rows.map { row ->
                        EventHistoryTableRow(
                            label = row.label,
                            values =
                                events.map { event ->
                                    row.displayValue(
                                        rawValue = eventDataValuesByUid[event.uid()]
                                            ?.get(row.dataElementUid)
                                            .orEmpty(),
                                        optionDisplayNamesBySet = optionDisplayNamesBySet,
                                    )
                                } + List(columns - events.size) { "" },
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
                )
            }
    }

    private fun rowDefinitions(
        programStageUid: String,
        excludedDataElementIds: Set<String>,
    ): List<HistoryTableSectionDefinition> {
        val stageDataElementsByUid =
            d2
                .programModule()
                .programStageDataElements()
                .withRenderType()
                .byProgramStage()
                .eq(programStageUid)
                .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
                .blockingGet()
                .mapNotNull { programStageDataElement ->
                    programStageDataElement.dataElement()?.uid()?.let { dataElementUid ->
                        dataElementUid to programStageDataElement
                    }
                }.toMap()

        val sections =
            d2
                .programModule()
                .programStageSections()
                .byProgramStageUid()
                .eq(programStageUid)
                .withDataElements()
                .blockingGet()
                .sortedWith(compareBy({ it.sortOrder() ?: Int.MAX_VALUE }, { it.uid() }))

        return if (sections.isEmpty()) {
            singleSectionRowDefinitions(
                programStageUid = programStageUid,
                excludedDataElementIds = excludedDataElementIds,
                stageDataElementsByUid = stageDataElementsByUid,
            )
        } else {
            sections.mapNotNull { section ->
                val rows =
                    section
                        .dataElements()
                        .orEmpty()
                        .mapNotNull { dataElement ->
                            dataElement.toRowDefinition(
                                stageDataElementsByUid = stageDataElementsByUid,
                                excludedDataElementIds = excludedDataElementIds,
                            )
                        }

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
        excludedDataElementIds: Set<String>,
        stageDataElementsByUid: Map<String, ProgramStageDataElement>,
    ): List<HistoryTableSectionDefinition> {
        val programStage =
            d2
                .programModule()
                .programStages()
                .uid(programStageUid)
                .blockingGet()
        val rows =
            stageDataElementsByUid
                .values
                .sortedWith(compareBy({ it.sortOrder() ?: Int.MAX_VALUE }, { it.dataElement()?.uid() }))
                .mapNotNull { programStageDataElement ->
                    programStageDataElement
                        .dataElement()
                        ?.toRowDefinition(
                            stageDataElementsByUid = stageDataElementsByUid,
                            excludedDataElementIds = excludedDataElementIds,
                        )
                }

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

    private fun DataElement.toRowDefinition(
        stageDataElementsByUid: Map<String, ProgramStageDataElement>,
        excludedDataElementIds: Set<String>,
    ): HistoryTableRowDefinition? {
        val uid = uid() ?: return null
        if (uid in excludedDataElementIds || uid !in stageDataElementsByUid) {
            return null
        }
        return HistoryTableRowDefinition(
            dataElementUid = uid,
            label = displayFormName() ?: displayName() ?: uid,
            valueType = valueType(),
            optionSetUid = optionSetUid(),
        )
    }

    private fun historyEvents(
        currentEvent: Event,
        maxColumns: Int,
    ): List<Event> {
        val enrollmentUid = currentEvent.enrollment()
        val stageUid = currentEvent.programStage() ?: return listOf(currentEvent)
        val currentEventDate = currentEvent.displayDate()

        val events =
            if (enrollmentUid.isNullOrBlank()) {
                listOf(currentEvent)
            } else {
                d2
                    .eventModule()
                    .events()
                    .withTrackedEntityDataValues()
                    .byEnrollmentUid()
                    .eq(enrollmentUid)
                    .byProgramStageUid()
                    .eq(stageUid)
                    .blockingGet()
            }

        return events
            .filter { event ->
                val eventDate = event.displayDate()
                event.uid() == currentEvent.uid() ||
                    currentEventDate == null ||
                    eventDate == null ||
                    !eventDate.after(currentEventDate)
            }.sortedWith(
                compareBy(
                    { event -> event.displayDate() ?: Date(0) },
                    { event -> event.uid() },
                ),
            ).takeLast(maxColumns)
    }

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

    companion object {
        private const val DEFAULT_COLUMN_COUNT = 12
        private const val HISTORY_TABLE_DATE_LABEL_FORMAT = "MMM d"
    }
}
