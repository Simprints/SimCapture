package org.dhis2.simprints

import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.dhis2.commons.simprints.usecases.SimprintsOrderSearchResultsByIdentifyResponseUseCase
import org.dhis2.data.search.SearchParametersModel
import org.dhis2.form.model.FieldUiModelImpl
import org.dhis2.mobile.commons.model.CustomIntentModel
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.dhis2.usescases.searchTrackEntity.SearchTeiModel
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.trackedentity.search.TrackedEntitySearchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SimprintsLoadBiometricSearchResultsUseCaseTest {
    private val searchRepository: SearchRepository = mock()
    private val searchRepositoryKt: SearchRepositoryKt = mock()
    private val orderSearchResultsByIdentifyResponse: SimprintsOrderSearchResultsByIdentifyResponseUseCase =
        mock()

    private val useCase =
        SimprintsLoadBiometricSearchResultsUseCase(
            searchRepository = searchRepository,
            searchRepositoryKt = searchRepositoryKt,
            orderSearchResultsByIdentifyResponse = orderSearchResultsByIdentifyResponse,
        )

    @Test
    fun `invoke should return ordered biometric search results when available`() =
        runTest {
            val firstItem = mock<TrackedEntitySearchItem>()
            val secondItem = mock<TrackedEntitySearchItem>()
            val firstModel = searchTeiModel("first")
            val secondModel = searchTeiModel("second")
            whenever(
                orderSearchResultsByIdentifyResponse(
                    any(),
                    any(),
                    any<suspend () -> List<TrackedEntitySearchItem>>(),
                ),
            ) doReturn listOf(secondItem, firstItem)
            whenever(searchRepository.transform(secondItem, null, false, null)) doReturn secondModel
            whenever(searchRepository.transform(firstItem, null, false, null)) doReturn firstModel

            val result =
                useCase(
                    searchItems = listOf(simprintsBiometricSearchField()),
                    searchParametersModel =
                        SearchParametersModel(
                            selectedProgram = null,
                            queryData = mutableMapOf("biometric" to listOf("guid-1", "guid-2")),
                        ),
                    isOnline = true,
                    offlineOnly = false,
                    sortingItem = null,
                )

            assertEquals(listOf(secondModel, firstModel), result?.asSnapshot())
            verify(searchRepository).transform(secondItem, null, false, null)
            verify(searchRepository).transform(firstItem, null, false, null)
        }

    @Test
    fun `invoke should return null when biometric ordering is not available`() =
        runTest {
            whenever(
                orderSearchResultsByIdentifyResponse(
                    any(),
                    any(),
                    any<suspend () -> List<TrackedEntitySearchItem>>(),
                ),
            ) doReturn null

            val result =
                useCase(
                    searchItems = listOf(simprintsBiometricSearchField()),
                    searchParametersModel =
                        SearchParametersModel(
                            selectedProgram = null,
                            queryData = mutableMapOf("biometric" to listOf("guid-1")),
                        ),
                    isOnline = true,
                    offlineOnly = false,
                    sortingItem = null,
                )

            assertNull(result)
        }

    private fun simprintsBiometricSearchField() =
        FieldUiModelImpl(
            uid = "biometric",
            label = "Biometric",
            value = "guid-1,guid-2",
            displayName = "guid-1,guid-2",
            autocompleteList = emptyList(),
            optionSetConfiguration = null,
            valueType = ValueType.TEXT,
            customIntent =
                CustomIntentModel(
                    uid = "identify-intent",
                    name = "Identify",
                    packageName = "com.simprints.id.IDENTIFY",
                    customIntentRequest = emptyList(),
                    customIntentResponse = emptyList(),
                ),
        )

    private fun searchTeiModel(header: String) =
        SearchTeiModel().apply {
            setHeader(header)
        }
}
