package org.dhis2.form.simprints.ramp.model

data class FormHistoryChart(
    val title: String,
    val labels: List<String>,
    val values: List<Float?>,
    val categories: List<FormHistoryChartCategory>? = null,
    val currentValueIndex: Int? = null,
    val displayMaxDecimalPlaces: Int? = null,
    val isYAxisInverted: Boolean = false,
) {
    fun withCurrentValue(value: String?): FormHistoryChart {
        val index = currentValueIndex?.takeIf { it in values.indices } ?: return this
        return copy(
            values =
                values.toMutableList().also { updatedValues ->
                    updatedValues[index] = value.toHistoryChartValue(categories)
                },
        )
    }
}

data class FormHistoryChartCategory(
    val storedValue: String,
    val name: String?,
    val displayName: String,
)

internal fun String?.toHistoryChartValue(categories: List<FormHistoryChartCategory>?): Float? =
    if (categories == null) {
        this?.toFloatOrNull()
    } else {
        this
            ?.let { value ->
                categories.indexOfFirst {
                    category -> value == category.storedValue || value == category.name || value == category.displayName
                }
            }
            ?.takeIf { index -> index >= 0 }
            ?.toFloat()
    }
