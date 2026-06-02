package org.dhis2.simprints.ramp.model

import org.hisp.dhis.android.core.common.ValueType

sealed interface EventHistoryTableUiState {
    data object Loading : EventHistoryTableUiState

    data class Success(
        val table: EventHistoryTable,
    ) : EventHistoryTableUiState

    data object Empty : EventHistoryTableUiState

    data class Error(
        val message: String? = null,
    ) : EventHistoryTableUiState
}

data class EventHistoryTable(
    val columns: List<EventHistoryTableColumn>,
    val sections: List<EventHistoryTableSection>,
    val dateRowValues: List<String> = emptyList(),
)

data class EventHistoryTableColumn(
    val eventUid: String?,
    val label: String,
)

data class EventHistoryTableSection(
    val title: String,
    val rows: List<EventHistoryTableRow>,
)

data class EventHistoryTableRow(
    val label: String,
    val values: List<EventHistoryTableCell>,
)

data class EventHistoryTableCell(
    val value: String,
    val valueType: ValueType? = null,
) {
    fun displayValue(
        yesLabel: String,
        noLabel: String,
    ): String =
        if (valueType == ValueType.BOOLEAN || valueType == ValueType.TRUE_ONLY) {
            when (value.toBooleanStrictOrNull()) {
                true -> yesLabel
                false -> noLabel
                null -> value
            }
        } else {
            value
        }
}
