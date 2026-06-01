package org.dhis2.form.simprints.ramp.model

data class FormHistoryChart(
    val title: String,
    val labels: List<String>,
    val values: List<Float?>,
    val currentValueIndex: Int? = null,
) {
    fun withCurrentValue(value: String?): FormHistoryChart {
        val index = currentValueIndex?.takeIf { it in values.indices } ?: return this
        return copy(
            values =
                values.toMutableList().also { updatedValues ->
                    updatedValues[index] = value?.toFloatOrNull()
                },
        )
    }
}
