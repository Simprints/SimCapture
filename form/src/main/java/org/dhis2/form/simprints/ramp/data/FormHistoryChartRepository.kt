package org.dhis2.form.simprints.ramp.data

import org.dhis2.commons.simprints.ramp.model.DataElementHistoryChartConfig
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.simprints.ramp.model.FormHistoryChart
import org.dhis2.form.simprints.ramp.model.FormHistoryChartCategory
import org.dhis2.form.simprints.ramp.model.toHistoryChartValue
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.event.Event
import java.util.Date

class FormHistoryChartRepository(
    private val eventUid: String,
    private val d2: D2,
) {
    private val followUpVisitEventsByProgramStage = mutableMapOf<String, List<Event>>()

    fun getChart(
        fieldUiModel: FieldUiModel,
        configs: List<DataElementHistoryChartConfig>,
    ): FormHistoryChart? {
        configs.firstOrNull { config ->
            config.dataElementId?.trim() == fieldUiModel.uid
        } ?: return null

        val currentEvent = loadCurrentEvent() ?: return null
        val chartConfig =
            configs.firstOrNull { config ->
                config.programId?.trim() == currentEvent.program() &&
                    config.followUpVisitProgramStageId?.trim() == currentEvent.programStage() &&
                    config.dataElementId?.trim() == fieldUiModel.uid
            } ?: return null

        return getVisitNumberHistoryChartFor(
            currentEvent = currentEvent,
            fieldUiModel = fieldUiModel,
            chartConfig = chartConfig,
        )
    }

    private fun getVisitNumberHistoryChartFor(
        currentEvent: Event,
        fieldUiModel: FieldUiModel,
        chartConfig: DataElementHistoryChartConfig,
    ): FormHistoryChart? {
        val followUpVisitMaxNumber = chartConfig.followUpVisitMaxNumber?.takeIf { it >= 0 } ?: return null
        val followUpVisitProgramStageUid = chartConfig.followUpVisitProgramStageId.trimToValue() ?: return null
        val xAxisVisitNumberDataElementUid = chartConfig.xAxisVisitNumberDataElementId.trimToValue() ?: return null
        val dataElementUid = chartConfig.dataElementId.trimToValue() ?: return null
        val labels = (0..followUpVisitMaxNumber).map(Int::toString)
        val values = MutableList<Float?>(labels.size) { null }
        val categories = getCategories(fieldUiModel.optionSet)
        var currentValueIndex: Int? = null

        getFollowUpVisitEvents(currentEvent, followUpVisitProgramStageUid).forEach { event ->
            val dataValuesByDataElement = event.dataValuesByDataElement()
            val visitNumber =
                dataValuesByDataElement[xAxisVisitNumberDataElementUid]
                    ?.toVisitNumber()
                    ?.takeIf { it in 0..followUpVisitMaxNumber }
                    ?: return@forEach
            val value =
                if (event.uid() == eventUid) {
                    currentValueIndex = visitNumber
                    fieldUiModel.value.toHistoryChartValue(categories)
                } else {
                    dataValuesByDataElement[dataElementUid].toHistoryChartValue(categories)
                }

            values[visitNumber] = value
        }

        return FormHistoryChart(
            title = fieldUiModel.label,
            labels = labels,
            values = values,
            categories = categories,
            currentValueIndex = currentValueIndex,
            isYAxisInverted = chartConfig.isYAxisInverted,
            displayMaxDecimalPlaces = chartConfig.displayMaxDecimalPlaces,
        )
    }

    private fun getCategories(optionSetUid: String?): List<FormHistoryChartCategory>? =
        optionSetUid.trimToValue()?.let { uid ->
            d2
                .optionModule()
                .options()
                .byOptionSetUid()
                .eq(uid)
                .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
                .blockingGet()
                .map { option ->
                    val storedValue = option.code().trimToValue() ?: option.uid()
                    FormHistoryChartCategory(
                        storedValue = storedValue,
                        name = option.name(),
                        displayName = option.displayName().trimToValue() ?: option.name().trimToValue() ?: storedValue,
                    )
                }
        }

    private fun loadCurrentEvent(): Event? =
        d2
            .eventModule()
            .events()
            .withTrackedEntityDataValues()
            .uid(eventUid)
            .blockingGet()

    private fun getFollowUpVisitEvents(
        currentEvent: Event,
        followUpVisitProgramStageUid: String,
    ): List<Event> {
        val enrollmentUid = currentEvent.enrollment()
        val currentEventDate = currentEvent.displayDate()
        val queriedEvents =
            if (enrollmentUid.isNullOrBlank()) {
                emptyList()
            } else {
                followUpVisitEventsByProgramStage[followUpVisitProgramStageUid]
                    ?: d2
                        .eventModule()
                        .events()
                        .withTrackedEntityDataValues()
                        .byEnrollmentUid()
                        .eq(enrollmentUid)
                        .byProgramStageUid()
                        .eq(followUpVisitProgramStageUid)
                        .byDeleted()
                        .isFalse
                        .blockingGet()
                        .also { followUpVisitEventsByProgramStage[followUpVisitProgramStageUid] = it }
            }
        val events =
            (queriedEvents + currentEvent)
                .associateBy { event -> event.uid() }
                .values

        return events
            .filter { followUpEvent ->
                val followUpEventDate = followUpEvent.displayDate()
                followUpEvent.uid() == eventUid ||
                    currentEventDate == null ||
                    followUpEventDate == null ||
                    !followUpEventDate.after(currentEventDate)
            }.sortedWith(
                compareBy(
                    { followUpEvent -> followUpEvent.displayDate() ?: Date(0) },
                    { followUpEvent -> followUpEvent.uid() },
                ),
            )
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

    private fun Event.displayDate(): Date? = eventDate() ?: dueDate() ?: created()

    private fun String?.trimToValue(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun String.toVisitNumber(): Int? {
        val number = trim().toDoubleOrNull() ?: return null
        val integer = number.toInt()
        return integer.takeIf { it.toDouble() == number }
    }
}
