package org.dhis2.commons.simprints.utils

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SimprintsExternalCredentialUtilsTest {
    @Test
    fun `externalCredentialValue reads SID credential without altering its value`() {
        val extras = extras("""{"type":"QRCode","value":"  001234  ","documentFields":{}}""")

        assertEquals("  001234  ", SimprintsExternalCredentialUtils.externalCredentialValue(extras))
    }

    @Test
    fun `externalCredentialValue ignores missing credentials`() {
        assertNull(SimprintsExternalCredentialUtils.externalCredentialValue(null))
        assertNull(SimprintsExternalCredentialUtils.externalCredentialValue(extras(null)))
    }

    @Test
    fun `externalCredentialValue does not coerce non-string values`() {
        listOf("1234", "true", "{}", "[]").forEach { value ->
            assertNull(SimprintsExternalCredentialUtils.externalCredentialValue(extras("""{"type":"QRCode","value":$value}""")))
        }
    }

    @Test
    fun `externalCredentialValue requires exact QRCode type`() {
        listOf("\"qrCode\"", "\"GhanaNhisCard\"", "null", "1234", "true", "{}", "[]").forEach { type ->
            assertNull(SimprintsExternalCredentialUtils.externalCredentialValue(extras("""{"type":$type,"value":"credential-1"}""")))
        }
        assertNull(SimprintsExternalCredentialUtils.externalCredentialValue(extras("""{"value":"credential-1"}""")))
    }

    @Test
    fun `externalCredentialValue ignores malformed and non-object JSON`() {
        listOf(
            "",
            "{",
            "{invalid",
            "null",
            "1234",
            "[]",
            "true",
            "\"credential-1\"",
            """{type:"QRCode",value:"credential-1"}""",
            """{"type":"QRCode","value":"credential-1"} trailing""",
        ).forEach { json ->
            assertNull(SimprintsExternalCredentialUtils.externalCredentialValue(extras(json)))
        }
    }

    @Test
    fun `enrollmentExternalCredentialValue accepts Enrol and Enrol Last results`() {
        listOf("com.simprints.id.REGISTER", "com.simprints.id.REGISTER_LAST_BIOMETRICS").forEach { action ->
            assertEquals(
                "credential-1",
                SimprintsExternalCredentialUtils.enrollmentExternalCredentialValue(
                    action = action,
                    guid = "guid-1",
                    extras = extras(),
                ),
            )
        }
    }

    @Test
    fun `enrollmentExternalCredentialValue rejects unrelated and non-enrollment actions`() {
        listOf(
            null,
            "",
            "other.app.REGISTER",
            "com.simprints.id.REGISTER_OTHER",
            "com.simprints.id.IDENTIFY",
            "com.simprints.id.VERIFY",
            "com.simprints.id.CONFIRM_IDENTITY",
        ).forEach { action ->
            assertNull(
                SimprintsExternalCredentialUtils.enrollmentExternalCredentialValue(
                    action = action,
                    guid = "guid-1",
                    extras = extras(),
                ),
            )
        }
    }

    @Test
    fun `enrollmentExternalCredentialValue requires nonblank mapped GUID`() {
        listOf(null, "", " \t\n").forEach { guid ->
            assertNull(
                SimprintsExternalCredentialUtils.enrollmentExternalCredentialValue(
                    action = "com.simprints.id.REGISTER",
                    guid = guid,
                    extras = extras(),
                ),
            )
        }
    }

    @Test
    fun `enrollmentExternalCredentialValue allows enrollment without a credential`() {
        assertNull(
            SimprintsExternalCredentialUtils.enrollmentExternalCredentialValue(
                action = "com.simprints.id.REGISTER",
                guid = "guid-1",
                extras = extras(null),
            ),
        )
    }

    private fun extras(
        credentialJson: String? = """{"type":"QRCode","value":"credential-1"}""",
        identificationKey: String? = null,
    ): Bundle =
        mock {
            on { getString("scannedCredential") } doReturn credentialJson
            on { containsKey("identification") } doReturn (identificationKey == "identification")
            on { containsKey("identifications") } doReturn (identificationKey == "identifications")
        }
}
