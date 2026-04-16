package org.dhis2.commons.simprints.usecases

import kotlinx.coroutines.runBlocking
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.commons.simprints.utils.SimprintsSearchUtils
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.trackedentity.TrackedEntityType
import org.hisp.dhis.android.core.trackedentity.search.TrackedEntitySearchItem
import org.hisp.dhis.android.core.trackedentity.search.TrackedEntitySearchItemAttribute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimprintsOrderSearchResultsByIdentifyResponseUseCaseTest {
    private val repository: SimprintsD2Repository = mock()
    private val useCase =
        SimprintsOrderSearchResultsByIdentifyResponseUseCase(
            simprintsD2Repository = repository,
        )

    @Test
    fun `invoke should order results by identify response values`() =
        runBlocking {
            val first = searchItem(uid = "tei-1", biometricGuid = "guid-1")
            val second = searchItem(uid = "tei-2", biometricGuid = "guid-2")

            val result =
                useCase(
                    searchFields = listOf(simprintsIdentifyField()),
                    queryData = mapOf("biometric" to listOf("guid-2", "guid-1")),
                    searchTrackedEntities = { listOf(first, second) },
                )

            assertEquals(listOf(second, first), result)
        }

    @Test
    fun `invoke should use stored attribute value when search item does not include biometric GUID value`() {
        runBlocking {
            val first = searchItem(uid = "tei-1")
            val second = searchItem(uid = "tei-2")
            val unmatched = searchItem(uid = "tei-3")
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-1",
                    "biometric",
                ),
            ) doReturn "guid-1"
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-2",
                    "biometric",
                ),
            ) doReturn "guid-2"
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-3",
                    "biometric",
                ),
            ) doReturn "unknown-guid"

            val result =
                useCase(
                    searchFields = listOf(simprintsIdentifyField()),
                    queryData = mapOf("biometric" to listOf("guid-2", "guid-1")),
                    searchTrackedEntities = { listOf(unmatched, first, second) },
                )

            assertEquals(listOf(second, first, unmatched), result)
            verify(repository).getTrackedEntityAttributeValue("tei-1", "biometric")
            verify(repository).getTrackedEntityAttributeValue("tei-2", "biometric")
            verify(repository).getTrackedEntityAttributeValue("tei-3", "biometric")
        }
    }

    @Test
    fun `invoke should return null without searching when identify response has fewer than two values`() =
        runBlocking {
            var searchWasCalled = false

            val result =
                useCase(
                    searchFields = listOf(simprintsIdentifyField()),
                    queryData = mapOf("biometric" to listOf("guid-1")),
                    searchTrackedEntities = {
                        searchWasCalled = true
                        emptyList()
                    },
                )

            assertNull(result)
            assertFalse(searchWasCalled)
        }

    @Test
    fun `invoke should return null without searching when field is not an identify callout`() =
        runBlocking {
            var searchWasCalled = false

            val result =
                useCase(
                    searchFields = listOf(nonSimprintsField()),
                    queryData = mapOf("biometric" to listOf("guid-2", "guid-1")),
                    searchTrackedEntities = {
                        searchWasCalled = true
                        emptyList()
                    },
                )

            assertNull(result)
            assertFalse(searchWasCalled)
        }

    private fun simprintsIdentifyField() =
        SimprintsSearchUtils.SearchField(
            uid = "biometric",
            value = null,
            customIntent = customIntent(packageName = "com.simprints.id.IDENTIFY"),
        )

    private fun nonSimprintsField() =
        SimprintsSearchUtils.SearchField(
            uid = "biometric",
            value = null,
            customIntent = customIntent(packageName = "com.example.OTHER"),
        )

    private fun customIntent(packageName: String) =
        CustomIntentModel(
            uid = packageName,
            name = packageName,
            packageName = packageName,
            customIntentRequest = emptyList(),
            customIntentResponse = emptyList(),
        )

    private fun searchItem(
        uid: String,
        biometricGuid: String? = null,
    ): TrackedEntitySearchItem =
        TrackedEntitySearchItem(
            uid = uid,
            created = null,
            lastUpdated = null,
            createdAtClient = null,
            lastUpdatedAtClient = null,
            organisationUnit = "orgUnit",
            geometry = null,
            syncState = null,
            aggregatedSyncState = null,
            deleted = false,
            type = TrackedEntityType.builder().uid("teiType").build(),
            header = uid,
            attributeValues =
                biometricGuid?.let {
                    listOf(
                        TrackedEntitySearchItemAttribute(
                            attribute = "biometric",
                            displayName = "Biometric",
                            displayFormName = "Biometric",
                            value = it,
                            created = null,
                            lastUpdated = null,
                            valueType = ValueType.TEXT,
                            displayInList = true,
                            optionSet = null,
                        ),
                    )
                } ?: emptyList(),
        )
}
