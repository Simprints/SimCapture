package org.dhis2.commons.simprints.utils

import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimprintsSearchUtilsTest {
    @Test
    fun `searchState should detect biometric identification query`() {
        val searchState =
            SimprintsSearchUtils.searchState(
                listOf(
                    SimprintsSearchUtils.SearchField(
                        uid = "biometric",
                        value = "guid-1",
                        customIntent = identifyIntent(),
                    ),
                ),
            )

        assertTrue(searchState.hasAnyQuery)
        assertTrue(searchState.hasBiometricIdentificationQuery)
        assertFalse(searchState.shouldClearPendingSession)
    }

    @Test
    fun `searchState should clear pending session for non biometric query`() {
        val searchState =
            SimprintsSearchUtils.searchState(
                listOf(
                    SimprintsSearchUtils.SearchField(
                        uid = "name",
                        value = "Alice",
                        customIntent = null,
                    ),
                ),
            )

        assertTrue(searchState.hasAnyQuery)
        assertFalse(searchState.hasBiometricIdentificationQuery)
        assertTrue(searchState.shouldClearPendingSession)
    }

    @Test
    fun `filterQueryData should remove biometric identification query`() {
        val filteredQueryData =
            SimprintsSearchUtils.filterQueryData(
                queryData =
                    mapOf(
                        "biometric" to listOf("guid-1"),
                        "name" to listOf("Alice"),
                        "empty" to emptyList(),
                    ),
                fields =
                    listOf(
                        SimprintsSearchUtils.SearchField(
                            uid = "biometric",
                            value = "guid-1",
                            customIntent = identifyIntent(),
                        ),
                        SimprintsSearchUtils.SearchField(
                            uid = "name",
                            value = "Alice",
                            customIntent = null,
                        ),
                    ),
            )

        assertEquals(hashMapOf("name" to listOf("Alice")), filteredQueryData)
    }

    @Test
    fun `shouldUseLastBiometricsLabel should require pending biometric session`() {
        assertTrue(
            SimprintsSearchUtils.shouldUseLastBiometricsLabel(
                searchState =
                    SimprintsSearchUtils.SearchState(
                        hasAnyQuery = true,
                        hasBiometricIdentificationQuery = true,
                    ),
                hasPendingSession = true,
            ),
        )
        assertFalse(
            SimprintsSearchUtils.shouldUseLastBiometricsLabel(
                searchState =
                    SimprintsSearchUtils.SearchState(
                        hasAnyQuery = true,
                        hasBiometricIdentificationQuery = false,
                    ),
                hasPendingSession = true,
            ),
        )
    }

    private fun identifyIntent() =
        CustomIntentModel(
            uid = "identify",
            name = "Identify",
            packageName = "com.simprints.id.IDENTIFY",
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
