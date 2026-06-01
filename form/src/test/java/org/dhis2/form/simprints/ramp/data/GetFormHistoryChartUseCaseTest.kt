package org.dhis2.form.simprints.ramp.data

import org.dhis2.commons.simprints.ramp.model.DataElementHistoryChartConfig
import org.dhis2.commons.simprints.ramp.model.RampDatastoreConfig
import org.dhis2.commons.simprints.ramp.repository.RampDatastoreRepository
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.simprints.ramp.model.FormHistoryChart
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetFormHistoryChartUseCaseTest {
    private val rampDatastoreRepository: RampDatastoreRepository = mock()
    private val formHistoryChartRepository: FormHistoryChartRepository = mock()
    private val useCase =
        GetFormHistoryChartUseCase(
            rampDatastoreRepository = rampDatastoreRepository,
            formHistoryChartRepository = formHistoryChartRepository,
        )

    @Test
    fun `invoke should build chart from ramp datastore chart configs`() {
        val fieldUiModel: FieldUiModel = mock()
        val chartConfigs =
            listOf(
                DataElementHistoryChartConfig(
                    programId = "program",
                    followUpVisitProgramStageId = "follow-stage",
                    dataElementId = "weight",
                    xAxisVisitNumberDataElementId = "visit-number",
                    followUpVisitMaxNumber = 3,
                ),
            )
        val expectedChart =
            FormHistoryChart(
                title = "Weight",
                labels = listOf("0", "1"),
                values = listOf(8f, 9f),
            )
        whenever(rampDatastoreRepository.getConfig()) doReturn
            RampDatastoreConfig(dataElementHistoryCharts = chartConfigs)
        whenever(formHistoryChartRepository.getChart(fieldUiModel, chartConfigs)) doReturn expectedChart

        val chart = useCase(fieldUiModel)

        assertEquals(expectedChart, chart)
        verify(rampDatastoreRepository).getConfig()
        verify(formHistoryChartRepository).getChart(fieldUiModel, chartConfigs)
    }
}
