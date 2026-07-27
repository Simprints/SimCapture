package org.dhis2.commons.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhis2.commons.ui.model.ListCardUiModel
import org.hisp.dhis.mobile.ui.designsystem.component.AdditionalInfoItem
import org.hisp.dhis.mobile.ui.designsystem.component.ListCard
import org.hisp.dhis.mobile.ui.designsystem.component.ListCardDescriptionModel
import org.hisp.dhis.mobile.ui.designsystem.component.ListCardTitleModel
import org.hisp.dhis.mobile.ui.designsystem.component.ProvideKeyValueItem
import org.hisp.dhis.mobile.ui.designsystem.component.ToggleInfoTextButton
import org.hisp.dhis.mobile.ui.designsystem.component.getKeyTrimmedText
import org.hisp.dhis.mobile.ui.designsystem.component.getKeyValueAnnotatedString
import org.hisp.dhis.mobile.ui.designsystem.component.state.rememberAdditionalInfoColumnState
import org.hisp.dhis.mobile.ui.designsystem.component.state.rememberListCardState
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.TextColor

@Composable
fun ListCardProvider(
    modifier: Modifier = Modifier,
    card: ListCardUiModel,
    title: ListCardTitleModel = ListCardTitleModel(text = card.title),
    @StringRes syncingResourceId: Int,
) {
    val emphasizedKey = card.emphasizedAdditionalInfoKey
    val hasCustomAdditionalInfo =
        emphasizedKey != null || card.styledAdditionalInfoValues.isNotEmpty()

    ListCard(
        modifier = modifier,
        listCardState =
            rememberListCardState(
                title = title,
                description = ListCardDescriptionModel(text = card.description),
                lastUpdated = card.lastUpdated,
                additionalInfoColumnState =
                    rememberAdditionalInfoColumnState(
                        additionalInfoList =
                            card.additionalInfo.takeUnless { hasCustomAdditionalInfo } ?: emptyList(),
                        syncProgressItem =
                            AdditionalInfoItem(
                                key = stringResource(id = syncingResourceId),
                                value = "",
                            ),
                        expandLabelText = card.expandLabelText,
                        shrinkLabelText = card.shrinkLabelText,
                    ),
                loading = false,
                expandable = false,
            ),
        listAvatar = card.avatar,
        onCardClick = card.onCardCLick,
        actionButton =
            if (!hasCustomAdditionalInfo) {
                card.actionButton
            } else {
                {
                    EmphasizedAdditionalInfoColumn(
                        additionalInfo = card.additionalInfo,
                        emphasizedKey = emphasizedKey,
                        styledValues = card.styledAdditionalInfoValues,
                        expandLabelText = card.expandLabelText,
                        shrinkLabelText = card.shrinkLabelText,
                    )
                    card.actionButton()
                }
            },
    )
}

@Composable
fun EmphasizedAdditionalInfoColumn(
    additionalInfo: List<AdditionalInfoItem>,
    emphasizedKey: String?,
    styledValues: Map<String, AnnotatedString> = emptyMap(),
    expandLabelText: String,
    shrinkLabelText: String,
    isDetailCard: Boolean = false,
) {
    val columnState =
        rememberAdditionalInfoColumnState(
            additionalInfoList = additionalInfo,
            syncProgressItem = AdditionalInfoItem(value = ""),
            expandLabelText = expandLabelText,
            shrinkLabelText = shrinkLabelText,
        )
    val visibleItems =
        if (columnState.isExpanded()) columnState.expandableItemList() else columnState.visibleExpandableItemList()

    Column(Modifier.testTag("LIST_CARD_ADDITIONAL_INFO_COLUMN")) {
        visibleItems.forEach {
            AdditionalInfoRow(
                item = it,
                emphasized = false,
                styledValue = it.key?.let(styledValues::get),
                isDetailCard = isDetailCard,
            )
        }
    }
    Column(Modifier.testTag("LIST_CARD_ADDITIONAL_INFO_CONSTANT_COLUMN")) {
        columnState.constantItemList().forEach {
            AdditionalInfoRow(
                item = it,
                emphasized = it.key == emphasizedKey,
                styledValue = it.key?.let(styledValues::get),
                isDetailCard = isDetailCard,
            )
        }
    }
    if (columnState.showExpandableContent()) {
        ToggleInfoTextButton(
            sectionState = columnState.currentSectionState(),
            shrinkLabelText = shrinkLabelText,
            expandLabelText = expandLabelText,
            onClick = columnState::updateSectionState,
        )
    }
}

@Composable
private fun AdditionalInfoRow(
    item: AdditionalInfoItem,
    emphasized: Boolean,
    styledValue: AnnotatedString?,
    isDetailCard: Boolean,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (emphasized || styledValue != null) {
            val keyText =
                getKeyTrimmedText(
                    item.key.orEmpty(),
                    maxWidth / 2 - Spacing.Spacing16,
                    rememberTextMeasurer(),
                )
            val keyValueText = getKeyValueAnnotatedString(keyText, item, isDetailCard)
            val text =
                buildAnnotatedString {
                    append(keyValueText)
                    if (emphasized) {
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), 0, length)
                    } else if (styledValue?.text == item.value) {
                        val valueStart = keyValueText.text.lastIndexOf(item.value)
                        if (valueStart >= 0) {
                            styledValue.spanStyles.forEach { range ->
                                addStyle(
                                    style = range.item,
                                    start = valueStart + range.start,
                                    end = valueStart + range.end,
                                )
                            }
                        }
                    }
                }
            Text(
                text = text,
                inlineContent =
                    mapOf(
                        "ItemIcon" to
                            InlineTextContent(
                                Placeholder(
                                    width = 20.sp,
                                    height = 20.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                                ),
                            ) {
                                Box(Modifier.size(20.dp)) {
                                    item.icon?.invoke()
                                }
                            },
                    ),
                color = item.color ?: TextColor.OnSurface,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight =
                            if (emphasized) {
                                FontWeight.Bold
                            } else {
                                MaterialTheme.typography.bodyMedium.fontWeight
                            },
                        lineHeight = 20.sp,
                    ),
                overflow = if (item.truncate) TextOverflow.Ellipsis else TextOverflow.Clip,
                maxLines = if (item.truncate) 2 else Int.MAX_VALUE,
            )
        } else {
            ProvideKeyValueItem(
                item,
                maxWidth / 2 - Spacing.Spacing16,
                isDetailCard,
            )
        }
    }
    Spacer(Modifier.size(if (isDetailCard) Spacing.Spacing8 else Spacing.Spacing4))
}
