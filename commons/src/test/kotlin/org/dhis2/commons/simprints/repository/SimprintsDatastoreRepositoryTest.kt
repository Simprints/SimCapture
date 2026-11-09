package org.dhis2.commons.simprints.repository

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

class SimprintsDatastoreRepositoryTest {
    private val d2: D2 = Mockito.mock(D2::class.java, Mockito.RETURNS_DEEP_STUBS)
    private val repository = SimprintsDatastoreRepository(d2)

    @Test
    fun `getConfig should trim configured identifiers and ignore invalid entries`() {
        stubConfigRawValue(
            """
            {
              "otherConfigs": [
                { "ignored": true }
              ],
              "programSpecificSettings": [
                {
                  "programId": " program ",
                  "externalCredentialAttributeId": " external-credential-attribute "
                },
                {
                  "externalCredentialAttributeId": "no-program"
                },
                {
                  "programId": " "
                }
              ]
            }
            """.trimIndent(),
        )

        val config = repository.getConfig()

        assertEquals("program", config.programSpecificSettings.single().programId)
        assertEquals(
            "external-credential-attribute",
            config.programSpecificSettings.single().externalCredentialAttributeId,
        )
    }

    @Test
    fun `external credential attribute should be returned only for configured program`() {
        stubConfigRawValue(
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
                .value("simprints", "biometrics")
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
                .value("simprints", "biometrics")
                .blockingGet(),
        ).thenThrow(RuntimeException("Datastore unavailable"))

        assertEquals(null, repository.externalCredentialAttributeId("program"))
    }

    @Test
    fun `getConfig should parse wrapped string datastore value`() {
        val rawJson =
            """
            {
              "programSpecificSettings": {
                "programId": "program",
                "externalCredentialAttributeId": "wrapped-string"
              }
            }
            """.trimIndent()
        stubConfigRawValue("JsonWrapper(json=${Gson().toJson(rawJson)})")

        val config = repository.getConfig()

        assertEquals("wrapped-string", config.programSpecificSettings.single().externalCredentialAttributeId)
    }

    @Test
    fun `getConfig should parse wrapped object datastore value`() {
        stubConfigRawValue("JsonWrapper(json=${getRawUnwrappedValue(externalCredentialAttributeId = "wrapped-object")})")

        val config = repository.getConfig()

        assertEquals("wrapped-object", config.programSpecificSettings.single().externalCredentialAttributeId)
    }

    @Test
    fun `getConfig should return default config when datastore value is invalid`() {
        stubConfigRawValue("{invalid")

        val config = repository.getConfig()

        assertEquals(0, config.programSpecificSettings.size)
        assertEquals(null, repository.externalCredentialAttributeId("program"))
    }

    @Test
    fun `getConfig should return default config when datastore value is a scalar string`() {
        stubConfigRawValue("JsonWrapper(json=\"string\")")

        val config = repository.getConfig()

        assertEquals(0, config.programSpecificSettings.size)
        assertEquals(null, repository.externalCredentialAttributeId("program"))
    }

    @Test
    fun `getConfig should propagate datastore read failure`() {
        whenever(
            d2
                .dataStoreModule()
                .dataStore()
                .value("simprints", "biometrics")
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
                .value("simprints", "biometrics")
                .blockingGet(),
        ) doReturnConsecutively
            listOf(
                datastoreEntry(getRawUnwrappedValue(externalCredentialAttributeId = "value1")),
                datastoreEntry(getRawUnwrappedValue(externalCredentialAttributeId = "value2")),
            )

        assertEquals(
            "value1",
            repository
                .getConfig()
                .programSpecificSettings
                .single()
                .externalCredentialAttributeId,
        )
        assertEquals(
            "value2",
            repository
                .getConfig()
                .programSpecificSettings
                .single()
                .externalCredentialAttributeId,
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

    private fun stubConfigRawValue(value: String) {
        whenever(
            d2
                .dataStoreModule()
                .dataStore()
                .value("simprints", "biometrics")
                .blockingGet(),
        ) doReturn datastoreEntry(value)
    }

    private fun datastoreEntry(value: String): DataStoreEntry =
        DataStoreEntry
            .builder()
            .namespace("simprints")
            .key("biometrics")
            .value(value)
            .build()

    private fun getRawUnwrappedValue(externalCredentialAttributeId: String) =
        """
        {
          "programSpecificSettings": {
            "programId": "program",
            "externalCredentialAttributeId": "$externalCredentialAttributeId"
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
