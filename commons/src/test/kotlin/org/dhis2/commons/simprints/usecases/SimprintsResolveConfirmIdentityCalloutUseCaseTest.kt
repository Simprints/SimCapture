package org.dhis2.commons.simprints.usecases

import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.mobile.commons.model.CustomIntentResponseDataModel
import org.dhis2.mobile.commons.model.CustomIntentResponseExtraType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimprintsResolveConfirmIdentityCalloutUseCaseTest {
    private val repository: SimprintsD2Repository = mock()

    @Test
    fun `invoke should return confirm identity callout when search and selected guid are available`() =
        runBlocking {
            val customIntent = identifyIntent()
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-uid",
                    "biometric",
                ),
            ) doReturn "selected-guid"
            val useCase =
                SimprintsResolveConfirmIdentityCalloutUseCase(
                    simprintsD2Repository = repository,
                )

            val intentActions = mutableListOf<String?>()
            Mockito.mockConstruction(Intent::class.java) { _, context ->
                intentActions.add(context.arguments().firstOrNull() as? String)
            }.use { construction ->
                val result =
                    useCase(
                        teiUid = "tei-uid",
                        searchFields =
                            listOf(
                                SimprintsSearchUtils.SearchField(
                                    uid = "biometric",
                                    value = "guid-1",
                                    customIntent = customIntent,
                                ),
                            ),
                        sessionId = "session-id",
                    )

                assertNotNull(result)
                val launchIntent = construction.constructed().single()
                assertEquals(listOf("com.simprints.id.CONFIRM_IDENTITY"), intentActions)
                assertSame(launchIntent, result!!.launchIntent)
                verify(launchIntent).putExtra("sessionId", "session-id")
                verify(launchIntent).putExtra("selectedGuid", "selected-guid")
                assertEquals(customIntent.customIntentResponse, result.responseData)
            }
        }

    @Test
    fun `invoke should return null when search value is blank`() =
        runBlocking {
            val useCase =
                SimprintsResolveConfirmIdentityCalloutUseCase(
                    simprintsD2Repository = repository,
                )

            val result =
                useCase(
                    teiUid = "tei-uid",
                    searchFields =
                        listOf(
                            SimprintsSearchUtils.SearchField(
                                uid = "biometric",
                                value = null,
                                customIntent = identifyIntent(),
                            ),
                        ),
                    sessionId = "session-id",
                )

            assertNull(result)
        }

    @Test
    fun `invoke should return confirm identity callout with blank search value when allowed`() =
        runBlocking {
            val customIntent = identifyIntent()
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-uid",
                    "biometric",
                ),
            ) doReturn "selected-guid"
            val useCase =
                SimprintsResolveConfirmIdentityCalloutUseCase(
                    simprintsD2Repository = repository,
                )

            val intentActions = mutableListOf<String?>()
            Mockito.mockConstruction(Intent::class.java) { _, context ->
                intentActions.add(context.arguments().firstOrNull() as? String)
            }.use { construction ->
                val result =
                    useCase(
                        teiUid = "tei-uid",
                        searchFields =
                            listOf(
                                SimprintsSearchUtils.SearchField(
                                    uid = "biometric",
                                    value = null,
                                    customIntent = customIntent,
                                ),
                            ),
                        sessionId = "session-id",
                        allowBlankSearchValue = true,
                    )

                assertNotNull(result)
                val launchIntent = construction.constructed().single()
                assertEquals(listOf("com.simprints.id.CONFIRM_IDENTITY"), intentActions)
                assertSame(launchIntent, result!!.launchIntent)
                verify(launchIntent).putExtra("sessionId", "session-id")
                verify(launchIntent).putExtra("selectedGuid", "selected-guid")
                assertEquals(customIntent.customIntentResponse, result.responseData)
            }
        }

    @Test
    fun `invoke should return null when selected guid is missing`() =
        runBlocking {
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-uid",
                    "biometric",
                ),
            ) doReturn null
            val useCase =
                SimprintsResolveConfirmIdentityCalloutUseCase(
                    simprintsD2Repository = repository,
                )

            val result =
                useCase(
                    teiUid = "tei-uid",
                    searchFields =
                        listOf(
                            SimprintsSearchUtils.SearchField(
                                uid = "biometric",
                                value = "guid-1",
                                customIntent = identifyIntent(),
                            ),
                        ),
                    sessionId = "session-id",
                )

            assertNull(result)
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
