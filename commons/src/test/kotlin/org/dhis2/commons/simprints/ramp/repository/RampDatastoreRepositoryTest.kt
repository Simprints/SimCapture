package org.dhis2.commons.simprints.ramp.repository

import com.google.gson.Gson
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.datastore.DataStoreEntry
import org.hisp.dhis.android.core.maintenance.D2Error
import org.hisp.dhis.android.core.maintenance.D2ErrorCode
import org.hisp.dhis.android.core.maintenance.D2ErrorComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RampDatastoreRepositoryTest {
    private val d2: D2 = Mockito.mock(D2::class.java, Mockito.RETURNS_DEEP_STUBS)
    private val repository = RampDatastoreRepository(d2)

    @Test
    fun `getConfig should parse history chart or table config and ignore invalid entries`() {
        stubRampConfigRawValue(
            """
            {
              "dataElementHistoryCharts": [
                {
                  "programId": "program",
                  "followUpVisitProgramStageId": "follow-stage",
                  "dataElementId": "weight",
                  "xAxisVisitNumberDataElementId": "visit-number",
                  "followUpVisitMaxNumber": 12
                },
                {
                  "programId": "program",
                  "followUpVisitProgramStageId": "follow-stage",
                  "xAxisVisitNumberDataElementId": "visit-number",
                  "followUpVisitMaxNumber": 12
                }
              ],
              "otherConfigs": [
                { "ignored": true }
              ],
              "programStageHistoryTable": [
                {
                  "programId": "program",
                  "followUpVisitProgramStageId": "follow-stage",
                  "followUpVisitMaxNumber": 12,
                  "headerVisitNumberDataElementId": "visit-number",
                  "excludedFollowUpVisitDataElementIds": ["excluded"]
                },
                {
                  "programId": "program",
                  "followUpVisitProgramStageId": "follow-stage",
                  "followUpVisitMaxNumber": 12
                }
              ]
            }
            """.trimIndent(),
        )

        val config = repository.getConfig()

        assertEquals(1, config.dataElementHistoryCharts.size)
        config.dataElementHistoryCharts.first().let { chart ->
            assertEquals("program", chart.programId)
            assertEquals("follow-stage", chart.followUpVisitProgramStageId)
            assertEquals("weight", chart.dataElementId)
            assertEquals("visit-number", chart.xAxisVisitNumberDataElementId)
            assertEquals(12, chart.followUpVisitMaxNumber)
        }
        assertEquals(1, config.programStageHistoryTables.size)
        config.programStageHistoryTables.first().let { table ->
            assertEquals("program", table.programId)
            assertEquals("follow-stage", table.followUpVisitProgramStageId)
            assertEquals(12, table.followUpVisitMaxNumber)
            assertEquals("visit-number", table.headerVisitNumberDataElementId)
            assertEquals(listOf("excluded"), table.excludedFollowUpVisitDataElementIds)
        }
    }

    @Test
    fun `getConfig should parse wrapped string datastore value`() {
        val rawJson =
            """
            {
              "dataElementHistoryCharts": {
                "programId": "program",
                "followUpVisitProgramStageId": "follow-stage",
                "dataElementId": "muac",
                "xAxisVisitNumberDataElementId": "visit-number",
                "followUpVisitMaxNumber": 5
              }
            }
            """.trimIndent()
        stubRampConfigRawValue("JsonWrapper(json=${Gson().toJson(rawJson)})")

        val config = repository.getConfig()

        assertEquals("muac", config.dataElementHistoryCharts.single().dataElementId)
    }

    @Test
    fun `getConfig should parse wrapped object datastore value`() {
        stubRampConfigRawValue("JsonWrapper(json=${getRampRawUnwrappedValue(dataElementId = "height")})")

        val config = repository.getConfig()

        assertEquals("height", config.dataElementHistoryCharts.single().dataElementId)
    }

    @Test
    fun `getConfig should return default config when datastore value is invalid`() {
        stubRampConfigRawValue("{invalid")

        val config = repository.getConfig()

        assertEquals(0, config.dataElementHistoryCharts.size)
        assertEquals(0, config.programStageHistoryTables.size)
    }

    @Test
    fun `getConfig should propagate datastore read failure`() {
        whenever(
            d2
                .dataStoreModule()
                .dataStore()
                .value("simprints", "ramp")
                .blockingGet(),
        ).thenAnswer { throw d2Error() }

        assertThrows(D2Error::class.java) {
            repository.getConfig()
        }
    }

    @Test
    fun `getConfig should reload config when local datastore value changes`() {
        whenever(
            d2
                .dataStoreModule()
                .dataStore()
                .value("simprints", "ramp")
                .blockingGet(),
        ) doReturnConsecutively
            listOf(
                rampEntry(getRampRawUnwrappedValue(dataElementId = "value1")),
                rampEntry(getRampRawUnwrappedValue(dataElementId = "value2")),
            )

        assertEquals(
            "value1",
            repository
                .getConfig()
                .dataElementHistoryCharts
                .single()
                .dataElementId,
        )
        assertEquals(
            "value2",
            repository
                .getConfig()
                .dataElementHistoryCharts
                .single()
                .dataElementId,
        )
    }

    @Test
    fun `sync should download simprints namespace`() {
        repository.sync()

        verify(
            d2
                .dataStoreModule()
                .dataStoreDownloader()
                .byNamespace()
                .eq("simprints"),
        ).blockingDownload()
    }

    private fun stubRampConfigRawValue(value: String) {
        whenever(
            d2
                .dataStoreModule()
                .dataStore()
                .value("simprints", "ramp")
                .blockingGet(),
        ) doReturn rampEntry(value)
    }

    private fun rampEntry(value: String): DataStoreEntry =
        DataStoreEntry
            .builder()
            .namespace("simprints")
            .key("ramp")
            .value(value)
            .build()

    private fun getRampRawUnwrappedValue(dataElementId: String) =
        """
        {
          "dataElementHistoryCharts": {
            "programId": "program",
            "followUpVisitProgramStageId": "follow-stage",
            "dataElementId": "$dataElementId",
            "xAxisVisitNumberDataElementId": "visit-number",
            "followUpVisitMaxNumber": 5
          }
        }
        """.trimIndent()

    private fun d2Error(): D2Error =
        D2Error
            .builder()
            .errorCode(D2ErrorCode.VALUE_CANT_BE_SET)
            .errorComponent(D2ErrorComponent.Database)
            .errorDescription("description")
            .build()
}
