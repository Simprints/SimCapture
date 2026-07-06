package org.dhis2.simprints.ramp.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dhis2.R
import org.dhis2.simprints.ramp.model.EventHistoryTable
import org.dhis2.simprints.ramp.model.EventHistoryTableCell
import org.dhis2.simprints.ramp.model.EventHistoryTableColumn
import org.dhis2.simprints.ramp.model.EventHistoryTableRow
import org.dhis2.simprints.ramp.model.EventHistoryTableUiState
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicator
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicatorType
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.dhis2.commons.R as CommonsR

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
                message = stringResource(R.string.simprints_ramp_history_table_empty),
            )

        is EventHistoryTableUiState.Error ->
            EventHistoryTableMessage(
                modifier = modifier,
                message = state.message ?: stringResource(R.string.something_wrong),
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
                .background(colorResource(id = R.color.white)),
    ) {
        VisitHeaderRow(
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
            if (table.dateRowValues.isNotEmpty()) {
                TableDataRow(
                    row =
                        EventHistoryTableRow(
                            label = stringResource(R.string.simprints_ramp_history_table_date),
                            values =
                                table.dateRowValues.map { value ->
                                    EventHistoryTableCell(value)
                                },
                        ),
                    horizontalScrollState = horizontalScrollState,
                )
            }
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
private fun VisitHeaderRow(
    columns: List<EventHistoryTableColumn>,
    horizontalScrollState: ScrollState,
) {
    Row(modifier = Modifier.heightIntrinsicRow()) {
        HeaderCell(
            text = stringResource(R.string.simprints_ramp_history_table_visit),
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
                .background(colorResource(id = R.color.bg_gray_f1f))
                .border(BorderWidth, colorResource(id = R.color.divider_bg))
                .padding(horizontal = Spacing.Spacing16, vertical = Spacing.Spacing8),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(id = R.color.blue_fab),
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
            row.values.forEach { cell ->
                ValueCell(
                    cell = cell,
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
                .background(colorResource(id = R.color.gray_f5f5))
                .border(BorderWidth, colorResource(id = R.color.divider_bg))
                .padding(horizontal = Spacing.Spacing8, vertical = Spacing.Spacing8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign,
            color = colorResource(id = R.color.text_black_333),
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
                .background(colorResource(id = R.color.form_field_background))
                .border(BorderWidth, colorResource(id = R.color.divider_bg))
                .padding(horizontal = Spacing.Spacing8, vertical = Spacing.Spacing8),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = colorResource(id = R.color.text_black_333),
        )
    }
}

@Composable
private fun ValueCell(
    cell: EventHistoryTableCell,
    width: Dp,
) {
    val text =
        cell.displayValue(
            yesLabel = stringResource(CommonsR.string.yes),
            noLabel = stringResource(CommonsR.string.no),
        )

    Box(
        modifier =
            Modifier
                .width(width)
                .fillMaxHeight()
                .defaultMinSize(minHeight = CellMinHeight)
                .background(colorResource(id = R.color.white))
                .border(BorderWidth, colorResource(id = R.color.divider_bg))
                .padding(horizontal = Spacing.Spacing8, vertical = Spacing.Spacing8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = colorResource(id = R.color.text_black_333),
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
                .background(colorResource(id = R.color.white))
                .padding(Spacing.Spacing24),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.text_black_333),
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
