package org.dhis2.simprints

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.dhis2.commons.filters.FilterManager
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.data.search.SearchParametersModel
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryKt
import org.dhis2.usescases.searchTrackEntity.SearchTeiModel
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.program.Program
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.search.TrackedEntitySearchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SimprintsResolveSingleBiometricSearchNavigationUseCaseTest {
    private val searchRepository: SearchRepository = mock()
    private val searchRepositoryKt: SearchRepositoryKt = mock()
    private val networkUtils: NetworkUtils = mock()
    private val filterManager: FilterManager = mock()

    @Test
    fun `invoke should return matched enrollment navigation when a single biometric result is found`() =
        runTest {
            val program = Program.builder().uid("initialProgramUid").build()
            val trackedEntity: TrackedEntitySearchItem = mock()
            val searchTeiModel =
                searchTeiModel(
                    teiUid = "teiUid",
                    enrollmentUid = "enrollmentUid",
                    enrollmentProgramUid = "matchedProgramUid",
                )
            whenever(searchRepository.getProgram("initialProgramUid")) doReturn program
            whenever(networkUtils.isOnline()) doReturn true
            whenever(filterManager.stateFilters) doReturn emptyList()
            whenever(
                searchRepositoryKt.searchTrackedEntitiesImmediate(
                    SearchParametersModel(
                        selectedProgram = program,
                        queryData = mutableMapOf("biometric" to listOf("guid-1")),
                    ),
                    true,
                ),
            ) doReturn listOf(trackedEntity)
            whenever(searchRepository.transform(trackedEntity, program, false, null)) doReturn
                searchTeiModel

            val result =
                useCase(StandardTestDispatcher(testScheduler))(
                    initialProgramUid = "initialProgramUid",
                    queryData = mapOf("biometric" to listOf("guid-1")),
                    value = "guid-1",
                )

            assertEquals(
                SimprintsResolveSingleBiometricSearchNavigationUseCase.NavigationTarget(
                    teiUid = "teiUid",
                    programUid = "matchedProgramUid",
                    enrollmentUid = "enrollmentUid",
                ),
                result,
            )
        }

    @Test
    fun `invoke should fall back to the initial program when the matched result has no selected enrollment`() =
        runTest {
            val program = Program.builder().uid("initialProgramUid").build()
            val trackedEntity: TrackedEntitySearchItem = mock()
            whenever(searchRepository.getProgram("initialProgramUid")) doReturn program
            whenever(networkUtils.isOnline()) doReturn false
            whenever(filterManager.stateFilters) doReturn emptyList()
            whenever(
                searchRepositoryKt.searchTrackedEntitiesImmediate(
                    SearchParametersModel(
                        selectedProgram = program,
                        queryData = mutableMapOf("biometric" to listOf("guid-1")),
                    ),
                    false,
                ),
            ) doReturn listOf(trackedEntity)
            whenever(searchRepository.transform(trackedEntity, program, true, null)) doReturn
                searchTeiModel(teiUid = "teiUid")

            val result =
                useCase(StandardTestDispatcher(testScheduler))(
                    initialProgramUid = "initialProgramUid",
                    queryData = mapOf("biometric" to listOf("guid-1")),
                    value = "guid-1",
                )

            assertEquals(
                SimprintsResolveSingleBiometricSearchNavigationUseCase.NavigationTarget(
                    teiUid = "teiUid",
                    programUid = "initialProgramUid",
                    enrollmentUid = null,
                ),
                result,
            )
        }

    @Test
    fun `invoke should return null when biometric value does not resolve to exactly one identifier`() =
        runTest {
            val result =
                useCase(StandardTestDispatcher(testScheduler))(
                    initialProgramUid = "initialProgramUid",
                    queryData = mapOf("biometric" to listOf("guid-1", "guid-2")),
                    value = "guid-1,guid-2",
                )

            assertNull(result)
            verify(searchRepositoryKt, never()).searchTrackedEntitiesImmediate(any(), any())
        }

    @Test
    fun `invoke should return null when search resolves multiple tracked entities`() =
        runTest {
            val program = Program.builder().uid("initialProgramUid").build()
            whenever(searchRepository.getProgram("initialProgramUid")) doReturn program
            whenever(networkUtils.isOnline()) doReturn true
            whenever(filterManager.stateFilters) doReturn emptyList()
            whenever(
                searchRepositoryKt.searchTrackedEntitiesImmediate(
                    SearchParametersModel(
                        selectedProgram = program,
                        queryData = mutableMapOf("biometric" to listOf("guid-1")),
                    ),
                    true,
                ),
            ) doReturn listOf(mock(), mock())

            val result =
                useCase(StandardTestDispatcher(testScheduler))(
                    initialProgramUid = "initialProgramUid",
                    queryData = mapOf("biometric" to listOf("guid-1")),
                    value = "guid-1",
                )

            assertNull(result)
            verify(searchRepository, never()).transform(any(), anyOrNull(), any(), anyOrNull())
        }

    private fun useCase(ioDispatcher: CoroutineDispatcher) =
        SimprintsResolveSingleBiometricSearchNavigationUseCase(
            searchRepository = searchRepository,
            searchRepositoryKt = searchRepositoryKt,
            networkUtils = networkUtils,
            filterManager = filterManager,
            ioDispatcher = ioDispatcher,
        )

    private fun searchTeiModel(
        teiUid: String,
        enrollmentUid: String? = null,
        enrollmentProgramUid: String? = null,
    ) = SearchTeiModel().apply {
        tei =
            TrackedEntityInstance
                .builder()
                .uid(teiUid)
                .trackedEntityType("teiType")
                .organisationUnit("orgUnit")
                .build()
        enrollmentUid?.let { uid ->
            setCurrentEnrollment(
                Enrollment
                    .builder()
                    .uid(uid)
                    .program(enrollmentProgramUid)
                    .build(),
            )
        }
    }
}
