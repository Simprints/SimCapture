package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dhis2.R
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicator
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicatorType
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing

@Composable
fun EventHistoryTableScreen(
    state: EventHistoryTableUiState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        EventHistoryTableUiState.Loading ->
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ProgressIndicator(type = ProgressIndicatorType.CIRCULAR)
            }

        EventHistoryTableUiState.Empty ->
            EventHistoryTableMessage(
                modifier = modifier,
                message = stringResource(R.string.history_table_empty),
            )

        is EventHistoryTableUiState.Error ->
            EventHistoryTableMessage(
                modifier = modifier,
                message = state.message ?: stringResource(R.string.error_unexpected_error),
            )

        is EventHistoryTableUiState.Success ->
            HistoryTable(
                table = state.table,
                modifier = modifier,
            )
    }
}

@Composable
private fun HistoryTable(
    table: EventHistoryTable,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White),
    ) {
        DateHeaderRow(
            columns = table.columns,
            horizontalScrollState = horizontalScrollState,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = Spacing.Spacing16),
        ) {
            table.sections.forEach { section ->
                SectionHeader(title = section.title)
                section.rows.forEach { row ->
                    TableDataRow(
                        row = row,
                        horizontalScrollState = horizontalScrollState,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateHeaderRow(
    columns: List<EventHistoryTableColumn>,
    horizontalScrollState: ScrollState,
) {
    Row(modifier = Modifier.heightIntrinsicRow()) {
        HeaderCell(
            text = stringResource(R.string.history_table_date),
            width = RowHeaderWidth,
            textAlign = TextAlign.Start,
        )
        Row(
            modifier =
                Modifier.horizontalScroll(
                    state = horizontalScrollState,
                    overscrollEffect = null,
                ),
        ) {
            columns.forEach { column ->
                HeaderCell(
                    text = column.label,
                    width = DataCellWidth,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(SectionHeaderColor)
                .border(BorderWidth, BorderColor)
                .padding(horizontal = Spacing.Spacing16, vertical = Spacing.Spacing8),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = SectionHeaderTextColor,
        )
    }
}

@Composable
private fun TableDataRow(
    row: EventHistoryTableRow,
    horizontalScrollState: ScrollState,
) {
    Row(modifier = Modifier.heightIntrinsicRow()) {
        LabelCell(
            text = row.label,
            width = RowHeaderWidth,
        )
        Row(
            modifier =
                Modifier.horizontalScroll(
                    state = horizontalScrollState,
                    overscrollEffect = null,
                ),
        ) {
            row.values.forEach { value ->
                ValueCell(
                    text = value,
                    width = DataCellWidth,
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: Dp,
    textAlign: TextAlign,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .fillMaxHeight()
                .defaultMinSize(minHeight = CellMinHeight)
                .background(HeaderColor)
                .border(BorderWidth, BorderColor)
                .padding(horizontal = Spacing.Spacing8, vertical = Spacing.Spacing8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign,
            color = HeaderTextColor,
        )
    }
}

@Composable
private fun LabelCell(
    text: String,
    width: Dp,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .fillMaxHeight()
                .defaultMinSize(minHeight = CellMinHeight)
                .background(LabelColor)
                .border(BorderWidth, BorderColor)
                .padding(horizontal = Spacing.Spacing8, vertical = Spacing.Spacing8),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = BodyTextColor,
        )
    }
}

@Composable
private fun ValueCell(
    text: String,
    width: Dp,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .fillMaxHeight()
                .defaultMinSize(minHeight = CellMinHeight)
                .background(Color.White)
                .border(BorderWidth, BorderColor)
                .padding(horizontal = Spacing.Spacing8, vertical = Spacing.Spacing8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = BodyTextColor,
        )
    }
}

@Composable
private fun EventHistoryTableMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(Spacing.Spacing24),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = BodyTextColor,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Modifier.heightIntrinsicRow(): Modifier =
    this
        .fillMaxWidth()
        .height(IntrinsicSize.Min)

private val RowHeaderWidth = 168.dp
private val DataCellWidth = 88.dp
private val CellMinHeight = 48.dp
private val BorderWidth = 0.5.dp
private val BorderColor = Color(0xFFE0E4EA)
private val HeaderColor = Color(0xFFF4F6F8)
private val LabelColor = Color(0xFFFAFBFC)
private val SectionHeaderColor = Color(0xFFEAF4FF)
private val SectionHeaderTextColor = Color(0xFF094067)
private val HeaderTextColor = Color(0xFF263238)
private val BodyTextColor = Color(0xFF263238)
