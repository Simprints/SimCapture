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
                  "followUpVisitMaxNumber": 12,
                  "isYAxisInverted": true,
                  "displayMaxDecimalPlaces": 1
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
            assertEquals(true, chart.isYAxisInverted)
            assertEquals(1, chart.displayMaxDecimalPlaces)
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
    fun `getConfig should trim configured identifiers across all RAMP config sections`() {
        stubRampConfigRawValue(
            """
            {
              "dataElementHistoryCharts": [
                {
                  "programId": " program ",
                  "followUpVisitProgramStageId": " follow-stage ",
                  "dataElementId": " weight ",
                  "xAxisVisitNumberDataElementId": " visit-number ",
                  "followUpVisitMaxNumber": 12
                }
              ],
              "programStageHistoryTable": [
                {
                  "programId": " program ",
                  "followUpVisitProgramStageId": " follow-stage ",
                  "followUpVisitMaxNumber": 12,
                  "headerVisitNumberDataElementId": " visit-number ",
                  "excludedFollowUpVisitDataElementIds": [" excluded ", " "]
                }
              ],
              "programSpecificSettings": [
                {
                  "programId": " disabledProgram ",
                  "isOneLevelUpOrgUnitForBiometricsModuleId": true,
                  "isBiometricsCaptureOnlyButtonEnabledForAttributeId": " biometrics ",
                  "externalCredentialAttributeId": " external-credential-attribute ",
                  "isSearchEnabled": false,
                  "isShowingUnfilteredList": false,
                  "hasDetailedEnrollmentListing": true,
                  "detailedEnrollmentListingDischargeOutcomeDataElementIds": [" outcome ", " "],
                  "detailedEnrollmentListingAdmissionProgramStageIds": [" admission ", " "],
                  "detailedEnrollmentListingDischargeProgramStageIds": [" discharge ", " "]
                }
              ],
              "programStageSpecificSettings": [
                {
                  "programStageId": " disabledStage ",
                  "hasVisitNumberPrefixForDateInList": true,
                  "visitNumberDataElementId": " visit-number ",
                  "isScheduleOptionEnabled": false,
                  "isReferOptionEnabled": false,
                  "isScheduleAnchoredToInitialVisit": true
                }
              ]
            }
            """.trimIndent(),
        )

        val config = repository.getConfig()

        config.dataElementHistoryCharts.single().let { chart ->
            assertEquals("program", chart.programId)
            assertEquals("follow-stage", chart.followUpVisitProgramStageId)
            assertEquals("weight", chart.dataElementId)
            assertEquals("visit-number", chart.xAxisVisitNumberDataElementId)
            assertEquals(false, chart.isYAxisInverted)
        }
        config.programStageHistoryTables.single().let { table ->
            assertEquals("program", table.programId)
            assertEquals("follow-stage", table.followUpVisitProgramStageId)
            assertEquals("visit-number", table.headerVisitNumberDataElementId)
            assertEquals(listOf("excluded"), table.excludedFollowUpVisitDataElementIds)
        }
        assertEquals("disabledProgram", config.programSpecificSettings.single().programId)
        assertEquals(
            true,
            config.programSpecificSettings.single()
                .isOneLevelUpOrgUnitForBiometricsModuleId,
        )
        assertEquals(
            "biometrics",
            config.programSpecificSettings.single()
                .isBiometricsCaptureOnlyButtonEnabledForAttributeId,
        )
        assertEquals(
            "external-credential-attribute",
            config.programSpecificSettings.single().externalCredentialAttributeId,
        )
        assertEquals(false, config.programSpecificSettings.single().isShowingUnfilteredList)
        assertEquals(true, config.programSpecificSettings.single().hasDetailedEnrollmentListing)
        assertEquals(
            listOf("outcome"),
            config.programSpecificSettings.single()
                .detailedEnrollmentListingDischargeOutcomeDataElementIds,
        )
        assertEquals(
            listOf("admission"),
            config.programSpecificSettings.single()
                .detailedEnrollmentListingAdmissionProgramStageIds,
        )
        assertEquals(
            listOf("discharge"),
            config.programSpecificSettings.single()
                .detailedEnrollmentListingDischargeProgramStageIds,
        )
        assertEquals("disabledStage", config.programStageSpecificSettings.single().programStageId)
        assertEquals(true, config.programStageSpecificSettings.single().hasVisitNumberPrefixForDateInList)
        assertEquals("visit-number", config.programStageSpecificSettings.single().visitNumberDataElementId)
        assertEquals(true, config.programStageSpecificSettings.single().isScheduleAnchoredToInitialVisit)
    }

    @Test
    fun `isSearchEnabled should return false only when program disables search`() {
        stubRampConfigRawValue(
            """
            {
              "programSpecificSettings": [
                {
                  "programId": " disabledProgram ",
                  "isSearchEnabled": false
                },
                {
                  "programId": "defaultProgram"
                },
                {
                  "programId": "enabledProgram",
                  "isSearchEnabled": true
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(false, repository.isSearchEnabled("disabledProgram"))
        assertEquals(false, repository.isSearchEnabled(" disabledProgram "))
        assertEquals(true, repository.isSearchEnabled("enabledProgram"))
        assertEquals(true, repository.isSearchEnabled("defaultProgram"))
        assertEquals(true, repository.isSearchEnabled("missingProgram"))
        assertEquals(true, repository.isSearchEnabled(null))
        assertEquals(true, repository.isSearchEnabled(" "))
    }

    @Test
    fun `isShowingUnfilteredList should return false only when program hides it`() {
        stubRampConfigRawValue(
            """
            {
              "programSpecificSettings": [
                {
                  "programId": " hiddenProgram ",
                  "isShowingUnfilteredList": false
                },
                {
                  "programId": "defaultProgram"
                },
                {
                  "programId": "visibleProgram",
                  "isShowingUnfilteredList": true
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(false, repository.isShowingUnfilteredList("hiddenProgram"))
        assertEquals(false, repository.isShowingUnfilteredList(" hiddenProgram "))
        assertEquals(true, repository.isShowingUnfilteredList("visibleProgram"))
        assertEquals(true, repository.isShowingUnfilteredList("defaultProgram"))
        assertEquals(true, repository.isShowingUnfilteredList("missingProgram"))
        assertEquals(true, repository.isShowingUnfilteredList(null))
        assertEquals(true, repository.isShowingUnfilteredList(" "))
    }

    @Test
    fun `one level up biometrics module id should be enabled only when program opts in`() {
        stubRampConfigRawValue(
            """
            {
              "programSpecificSettings": [
                {
                  "programId": " enabledProgram ",
                  "isOneLevelUpOrgUnitForBiometricsModuleId": true
                },
                {
                  "programId": "disabledProgram",
                  "isOneLevelUpOrgUnitForBiometricsModuleId": false
                },
                {
                  "programId": "defaultProgram"
                },
                {
                  "programId": "nullProgram",
                  "isOneLevelUpOrgUnitForBiometricsModuleId": null
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(true, repository.isOneLevelUpOrgUnitForBiometricsModuleId("enabledProgram"))
        assertEquals(true, repository.isOneLevelUpOrgUnitForBiometricsModuleId(" enabledProgram "))
        assertEquals(false, repository.isOneLevelUpOrgUnitForBiometricsModuleId("disabledProgram"))
        assertEquals(false, repository.isOneLevelUpOrgUnitForBiometricsModuleId("defaultProgram"))
        assertEquals(false, repository.isOneLevelUpOrgUnitForBiometricsModuleId("nullProgram"))
        assertEquals(false, repository.isOneLevelUpOrgUnitForBiometricsModuleId("missingProgram"))
        assertEquals(false, repository.isOneLevelUpOrgUnitForBiometricsModuleId(null))
        assertEquals(false, repository.isOneLevelUpOrgUnitForBiometricsModuleId(" "))
    }

    @Test
    fun `biometrics capture only attribute should be returned only for configured program`() {
        stubRampConfigRawValue(
            """
            {
              "programSpecificSettings": [
                {
                  "programId": " enabledProgram ",
                  "isBiometricsCaptureOnlyButtonEnabledForAttributeId": " biometrics "
                },
                {
                  "programId": "blankAttributeProgram",
                  "isBiometricsCaptureOnlyButtonEnabledForAttributeId": " "
                },
                {
                  "programId": "defaultProgram"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("biometrics", repository.biometricsCaptureOnlyAttributeId("enabledProgram"))
        assertEquals("biometrics", repository.biometricsCaptureOnlyAttributeId(" enabledProgram "))
        assertEquals(null, repository.biometricsCaptureOnlyAttributeId("blankAttributeProgram"))
        assertEquals(null, repository.biometricsCaptureOnlyAttributeId("defaultProgram"))
        assertEquals(null, repository.biometricsCaptureOnlyAttributeId("missingProgram"))
        assertEquals(null, repository.biometricsCaptureOnlyAttributeId(null))
        assertEquals(null, repository.biometricsCaptureOnlyAttributeId(" "))
    }

    @Test
    fun `external credential attribute should be returned only for configured program`() {
        stubRampConfigRawValue(
            """
            {
              "programSpecificSettings": [
                {
                  "programId": " enabledProgram ",
                  "externalCredentialAttributeId": " external-credential-attribute "
                },
                {
                  "programId": "otherProgram",
                  "externalCredentialAttributeId": "other-attribute"
                },
                {
                  "programId": "blankAttributeProgram",
                  "externalCredentialAttributeId": " "
                },
                {
                  "programId": "nullAttributeProgram",
                  "externalCredentialAttributeId": null
                },
                {
                  "programId": "defaultProgram"
                },
                {
                  "programId": " ",
                  "externalCredentialAttributeId": "invalid-program-attribute"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("external-credential-attribute", repository.externalCredentialAttributeId("enabledProgram"))
        assertEquals("external-credential-attribute", repository.externalCredentialAttributeId(" enabledProgram "))
        assertEquals("other-attribute", repository.externalCredentialAttributeId("otherProgram"))
        assertEquals(null, repository.externalCredentialAttributeId("blankAttributeProgram"))
        assertEquals(null, repository.externalCredentialAttributeId("nullAttributeProgram"))
        assertEquals(null, repository.externalCredentialAttributeId("defaultProgram"))
        assertEquals(null, repository.externalCredentialAttributeId("missingProgram"))
        assertEquals(null, repository.externalCredentialAttributeId(null))
        assertEquals(null, repository.externalCredentialAttributeId(" "))
    }

    @Test
    fun `external credential attribute should be absent when datastore is missing`() {
        whenever(
            d2
                .dataStoreModule()
                .dataStore()
                .value("simprints", "ramp")
                .blockingGet(),
        ).thenReturn(null)

        assertEquals(null, repository.externalCredentialAttributeId("program"))
    }

    @Test
    fun `external credential attribute should be absent when datastore read fails`() {
        whenever(
            d2
                .dataStoreModule()
                .dataStore()
                .value("simprints", "ramp")
                .blockingGet(),
        ).thenThrow(RuntimeException("Datastore unavailable"))

        assertEquals(null, repository.externalCredentialAttributeId("program"))
    }

    @Test
    fun `detailed enrollment listing should be enabled only for configured program`() {
        stubRampConfigRawValue(
            """
            {
              "programSpecificSettings": [
                {
                  "programId": "program",
                  "hasDetailedEnrollmentListing": true,
                  "detailedEnrollmentListingDischargeOutcomeDataElementIds": [
                    "outcome1",
                    "outcome2"
                  ],
                  "detailedEnrollmentListingAdmissionProgramStageIds": [
                    "admissionStage1",
                    "admissionStage2"
                  ],
                  "detailedEnrollmentListingDischargeProgramStageIds": [
                    "dischargeStage1",
                    "dischargeStage2"
                  ]
                },
                {
                  "programId": "disabledProgram",
                  "hasDetailedEnrollmentListing": false
                }
              ]
            }
            """.trimIndent(),
        )

        val settings = repository.detailedEnrollmentListingSettings("program")

        assertEquals(
            setOf("outcome1", "outcome2"),
            settings?.dischargeOutcomeDataElementIds,
        )
        assertEquals(
            setOf("admissionStage1", "admissionStage2"),
            settings?.admissionProgramStageIds,
        )
        assertEquals(
            setOf("dischargeStage1", "dischargeStage2"),
            settings?.dischargeProgramStageIds,
        )
        assertEquals(null, repository.detailedEnrollmentListingSettings("disabledProgram"))
        assertEquals(null, repository.detailedEnrollmentListingSettings("missingProgram"))
        assertEquals(null, repository.detailedEnrollmentListingSettings(null))
        assertEquals(null, repository.detailedEnrollmentListingSettings(" "))
    }

    @Test
    fun `program stage options should return false only when stage disables them`() {
        stubRampConfigRawValue(
            """
            {
              "programStageSpecificSettings": [
                {
                  "programStageId": " disabledStage ",
                  "isScheduleOptionEnabled": false,
                  "isReferOptionEnabled": false
                },
                {
                  "programStageId": "defaultStage"
                },
                {
                  "programStageId": "enabledStage",
                  "isScheduleOptionEnabled": true,
                  "isReferOptionEnabled": true
                },
                {
                  "isScheduleOptionEnabled": false,
                  "isReferOptionEnabled": false
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(false, repository.isScheduleOptionEnabled("disabledStage"))
        assertEquals(false, repository.isReferOptionEnabled("disabledStage"))
        assertEquals(false, repository.isScheduleOptionEnabled(" disabledStage "))
        assertEquals(false, repository.isReferOptionEnabled(" disabledStage "))
        assertEquals(true, repository.isScheduleOptionEnabled("enabledStage"))
        assertEquals(true, repository.isReferOptionEnabled("enabledStage"))
        assertEquals(true, repository.isScheduleOptionEnabled("defaultStage"))
        assertEquals(true, repository.isReferOptionEnabled("defaultStage"))
        assertEquals(true, repository.isScheduleOptionEnabled("missingStage"))
        assertEquals(true, repository.isReferOptionEnabled("missingStage"))
        assertEquals(true, repository.isScheduleOptionEnabled(null))
        assertEquals(true, repository.isReferOptionEnabled(null))
        assertEquals(true, repository.isScheduleOptionEnabled(" "))
        assertEquals(true, repository.isReferOptionEnabled(" "))
    }

    @Test
    fun `anchored schedule should return visit number data element only for enabled stage`() {
        stubRampConfigRawValue(
            """
            {
              "programStageSpecificSettings": [
                {
                  "programStageId": "anchoredStage",
                  "visitNumberDataElementId": "visit-number",
                  "isScheduleAnchoredToInitialVisit": true
                },
                {
                  "programStageId": "disabledStage",
                  "visitNumberDataElementId": "visit-number",
                  "isScheduleAnchoredToInitialVisit": false
                },
                {
                  "programStageId": "defaultStage",
                  "visitNumberDataElementId": "visit-number"
                },
                {
                  "programStageId": "missingVisitNumber",
                  "isScheduleAnchoredToInitialVisit": true
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(
            "visit-number",
            repository.anchoredScheduleVisitNumberDataElementId(" anchoredStage "),
        )
        assertEquals(null, repository.anchoredScheduleVisitNumberDataElementId("disabledStage"))
        assertEquals(null, repository.anchoredScheduleVisitNumberDataElementId("defaultStage"))
        assertEquals(null, repository.anchoredScheduleVisitNumberDataElementId("missingVisitNumber"))
        assertEquals(null, repository.anchoredScheduleVisitNumberDataElementId("missingStage"))
        assertEquals(null, repository.anchoredScheduleVisitNumberDataElementId(null))
        assertEquals(null, repository.anchoredScheduleVisitNumberDataElementId(" "))
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
        assertEquals(false, repository.isOneLevelUpOrgUnitForBiometricsModuleId("program"))
        assertEquals(null, repository.biometricsCaptureOnlyAttributeId("program"))
        assertEquals(null, repository.externalCredentialAttributeId("program"))
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
