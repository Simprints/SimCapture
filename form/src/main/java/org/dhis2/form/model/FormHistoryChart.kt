package org.dhis2.form.model

data class FormHistoryChart(
    val title: String,
    val labels: List<String>,
    val values: List<Float?>,
)
