package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.history

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
    val columnHeaderType: EventHistoryTableColumnHeaderType = EventHistoryTableColumnHeaderType.DATE,
    val dateRowValues: List<String> = emptyList(),
)

enum class EventHistoryTableColumnHeaderType {
    DATE,
    VISIT,
}

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
    val values: List<String>,
)
