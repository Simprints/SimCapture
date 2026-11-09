package org.dhis2.commons.simprints.usecases

import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.dhis2.commons.simprints.repository.SimprintsD2Repository
import org.dhis2.mobile.commons.customintents.CustomIntentRepository
import org.dhis2.mobile.commons.model.CustomIntentActionTypeModel
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

class SimprintsResolvePendingEnrollmentActionUseCaseTest {
    private val repository: SimprintsD2Repository = mock()
    private val customIntentRepository: CustomIntentRepository = mock()

    @Test
    fun `invoke should return pending action when register last attribute has no value`() =
        runBlocking {
            val customIntent = registerIntent()
            whenever(repository.getEnrollmentContext("enrollment-uid")) doReturn
                SimprintsD2Repository.EnrollmentContext(
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    orgUnitUid = "org-unit-uid",
                )
            whenever(repository.getProgramAttributeUids("program-uid")) doReturn listOf("program-attribute")
            whenever(repository.getTrackedEntityTypeAttributeUids("tei-uid")) doReturn emptyList()
            whenever(
                customIntentRepository.getCustomIntent(
                    "program-attribute",
                    "org-unit-uid",
                    CustomIntentActionTypeModel.DATA_ENTRY,
                ),
            ) doReturn customIntent
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-uid",
                    "program-attribute",
                ),
            ) doReturn null

            val useCase =
                SimprintsResolvePendingEnrollmentActionUseCase(
                    simprintsD2Repository = repository,
                    customIntentRepository = customIntentRepository,
                )

            val intentActions = mutableListOf<String?>()
            Mockito.mockConstruction(Intent::class.java) { _, context ->
                intentActions.add(context.arguments().firstOrNull() as? String)
            }.use { construction ->
                val result =
                    useCase(
                        enrollmentUid = "enrollment-uid",
                        sessionId = "session-id",
                    )

                assertNotNull(result)
                val launchIntent = construction.constructed().single()
                assertEquals("program-attribute", result!!.fieldUid)
                assertEquals(listOf("com.simprints.id.REGISTER_LAST_BIOMETRICS"), intentActions)
                assertSame(launchIntent, result.callout.launchIntent)
                verify(launchIntent).putExtra("sessionId", "session-id")
                assertEquals(customIntent.customIntentResponse, result.callout.responseData)
            }
        }

    @Test
    fun `invoke should return null when only identify callout is available`() =
        runBlocking {
            whenever(repository.getEnrollmentContext("enrollment-uid")) doReturn
                SimprintsD2Repository.EnrollmentContext(
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    orgUnitUid = "org-unit-uid",
                )
            whenever(repository.getProgramAttributeUids("program-uid")) doReturn listOf("program-attribute")
            whenever(repository.getTrackedEntityTypeAttributeUids("tei-uid")) doReturn emptyList()
            whenever(
                customIntentRepository.getCustomIntent(
                    "program-attribute",
                    "org-unit-uid",
                    CustomIntentActionTypeModel.DATA_ENTRY,
                ),
            ) doReturn identifyIntent()
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-uid",
                    "program-attribute",
                ),
            ) doReturn null

            val useCase =
                SimprintsResolvePendingEnrollmentActionUseCase(
                    simprintsD2Repository = repository,
                    customIntentRepository = customIntentRepository,
                )
            val result =
                useCase(
                    enrollmentUid = "enrollment-uid",
                    sessionId = "session-id",
                )

            assertNull(result)
        }

    @Test
    fun `invoke should return null when attribute already has a value`() =
        runBlocking {
            whenever(repository.getEnrollmentContext("enrollment-uid")) doReturn
                SimprintsD2Repository.EnrollmentContext(
                    teiUid = "tei-uid",
                    programUid = "program-uid",
                    orgUnitUid = "org-unit-uid",
                )
            whenever(repository.getProgramAttributeUids("program-uid")) doReturn listOf("program-attribute")
            whenever(repository.getTrackedEntityTypeAttributeUids("tei-uid")) doReturn listOf("tei-attribute")
            whenever(
                customIntentRepository.getCustomIntent(
                    "program-attribute",
                    "org-unit-uid",
                    CustomIntentActionTypeModel.DATA_ENTRY,
                ),
            ) doReturn registerIntent()
            whenever(
                repository.getTrackedEntityAttributeValue(
                    "tei-uid",
                    "program-attribute",
                ),
            ) doReturn "existing-guid"

            val useCase =
                SimprintsResolvePendingEnrollmentActionUseCase(
                    simprintsD2Repository = repository,
                    customIntentRepository = customIntentRepository,
                )
            val result =
                useCase(
                    enrollmentUid = "enrollment-uid",
                    sessionId = "session-id",
                )

            assertNull(result)
        }

    @Test
    fun `invoke should return null when enrollment context is missing`() =
        runBlocking {
            whenever(repository.getEnrollmentContext("enrollment-uid")) doReturn null
            val useCase =
                SimprintsResolvePendingEnrollmentActionUseCase(
                    simprintsD2Repository = repository,
                    customIntentRepository = customIntentRepository,
                )

            val result =
                useCase(
                    enrollmentUid = "enrollment-uid",
                    sessionId = "session-id",
                )

            assertNull(result)
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
