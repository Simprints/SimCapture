package org.dhis2.commons.simprints.utils

import android.os.Bundle
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SimprintsIntentUtilsTest {
    @Test
    fun `hasPendingValue should require empty value and register intent and pending enrollment`() {
        assertTrue(
            SimprintsIntentUtils.hasPendingValue(
                customIntent = registerIntent(),
                value = null,
                hasPendingEnrollment = true,
            ),
        )
        assertFalse(
            SimprintsIntentUtils.hasPendingValue(
                customIntent = registerIntent(),
                value = null,
                hasPendingEnrollment = false,
            ),
        )
        assertFalse(
            SimprintsIntentUtils.hasPendingValue(
                customIntent = registerIntent(),
                value = "guid-1",
                hasPendingEnrollment = true,
            ),
        )
        assertFalse(
            SimprintsIntentUtils.hasPendingValue(
                customIntent = identifyIntent(),
                value = null,
                hasPendingEnrollment = true,
            ),
        )
    }

    @Test
    fun `getDisplayValues should prefer stored value then pending placeholder`() {
        assertEquals(
            listOf("guid-1", "guid-2"),
            SimprintsIntentUtils.getDisplayValues(
                value = "guid-1,guid-2",
                hasPendingValue = true,
                placeholderValue = "From last biometric search",
            ),
        )
        assertEquals(
            listOf("From last biometric search"),
            SimprintsIntentUtils.getDisplayValues(
                value = null,
                hasPendingValue = true,
                placeholderValue = "From last biometric search",
            ),
        )
        assertTrue(
            SimprintsIntentUtils
                .getDisplayValues(
                    value = null,
                    hasPendingValue = false,
                    placeholderValue = "From last biometric search",
                ).isEmpty(),
        )
    }

    @Test
    fun `extractSessionId should read session id extra`() {
        val extras =
            mock<Bundle> {
                on { getString("sessionId") } doReturn "session-id"
            }

        val sessionId = SimprintsIntentUtils.extractSessionId(extras)

        assertEquals("session-id", sessionId)
    }

    private fun identifyIntent() = customIntent(packageName = "com.simprints.id.IDENTIFY")

    private fun registerIntent() = customIntent(packageName = "com.simprints.id.REGISTER")

    private fun customIntent(packageName: String) =
        CustomIntentModel(
            uid = packageName,
            name = packageName,
            packageName = packageName,
            customIntentRequest = emptyList(),
            customIntentResponse =
                listOf(
                    CustomIntentResponseDataModel(
                        name = "guid",
                        extraType = CustomIntentResponseExtraType.STRING,
                        key = null,
                    ),
                ),
        )
}
