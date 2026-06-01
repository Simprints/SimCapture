package org.dhis2.form.simprints.ramp.data

import org.dhis2.commons.simprints.ramp.repository.RampDatastoreRepository
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.simprints.ramp.model.FormHistoryChart

class GetFormHistoryChartUseCase(
    private val rampDatastoreRepository: RampDatastoreRepository,
    private val formHistoryChartRepository: FormHistoryChartRepository,
) {
    operator fun invoke(fieldUiModel: FieldUiModel): FormHistoryChart? =
        formHistoryChartRepository.getChart(
            fieldUiModel = fieldUiModel,
            configs = rampDatastoreRepository.getConfig().dataElementHistoryCharts,
        )
}
