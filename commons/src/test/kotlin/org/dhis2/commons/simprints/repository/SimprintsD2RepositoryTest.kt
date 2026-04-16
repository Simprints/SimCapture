package org.dhis2.commons.simprints.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.filters.internal.StringFilterConnector
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttribute
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttributeCollectionRepository
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValueObjectRepository
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.TrackedEntityTypeAttribute
import org.hisp.dhis.android.core.trackedentity.TrackedEntityTypeAttributeCollectionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimprintsD2RepositoryTest {
    private val d2: D2 = Mockito.mock(D2::class.java, Mockito.RETURNS_DEEP_STUBS)
    private val repository = SimprintsD2Repository(d2, Dispatchers.Unconfined)

    @Test
    fun `getEnrollmentContext should map enrollment fields`() = runBlocking {
        whenever(
            d2
                .enrollmentModule()
                .enrollments()
                .uid("enrollment-uid")
                .blockingGet(),
        ) doReturn
            Enrollment
                .builder()
                .uid("enrollment-uid")
                .trackedEntityInstance("tei-uid")
                .program("program-uid")
                .organisationUnit("org-unit-uid")
                .build()

        val result = repository.getEnrollmentContext("enrollment-uid")

        assertEquals(
            SimprintsD2Repository.EnrollmentContext(
                teiUid = "tei-uid",
                programUid = "program-uid",
                orgUnitUid = "org-unit-uid",
            ),
            result,
        )
    }

    @Test
    fun `getEnrollmentContext should return null when enrollment is missing`() = runBlocking {
        whenever(
            d2
                .enrollmentModule()
                .enrollments()
                .uid("enrollment-uid")
                .blockingGet(),
        ) doReturn null

        val result = repository.getEnrollmentContext("enrollment-uid")

        assertNull(result)
    }

    @Test
    fun `getEnrollmentContext should return null when tracked entity instance is missing`() = runBlocking {
        whenever(
            d2
                .enrollmentModule()
                .enrollments()
                .uid("enrollment-uid")
                .blockingGet(),
        ) doReturn
            Enrollment
                .builder()
                .uid("enrollment-uid")
                .program("program-uid")
                .organisationUnit("org-unit-uid")
                .build()

        val result = repository.getEnrollmentContext("enrollment-uid")

        assertNull(result)
    }

    @Test
    fun `getProgramAttributeUids should map non null tracked entity attributes`() = runBlocking {
        val programFilter: StringFilterConnector<ProgramTrackedEntityAttributeCollectionRepository> =
            mock()
        val programAttributeRepository: ProgramTrackedEntityAttributeCollectionRepository = mock()
        whenever(
            d2.programModule().programTrackedEntityAttributes().byProgram(),
        ) doReturn programFilter
        whenever(programFilter.eq("program-uid")) doReturn programAttributeRepository
        whenever(programAttributeRepository.blockingGet()) doReturn
            listOf(
                ProgramTrackedEntityAttribute
                    .builder()
                    .uid("program-attribute-1")
                    .trackedEntityAttribute(ObjectWithUid.create("attribute-1"))
                    .build(),
                ProgramTrackedEntityAttribute
                    .builder()
                    .uid("program-attribute-2")
                    .build(),
                ProgramTrackedEntityAttribute
                    .builder()
                    .uid("program-attribute-3")
                    .trackedEntityAttribute(ObjectWithUid.create("attribute-3"))
                    .build(),
            )

        val result = repository.getProgramAttributeUids("program-uid")

        assertEquals(listOf("attribute-1", "attribute-3"), result)
    }

    @Test
    fun `getTrackedEntityTypeAttributeUids should map type attribute uids`() = runBlocking {
        whenever(
            d2
                .trackedEntityModule()
                .trackedEntityInstances()
                .uid("tei-uid")
                .blockingGet(),
        ) doReturn
            TrackedEntityInstance
                .builder()
                .uid("tei-uid")
                .trackedEntityType("tracked-entity-type-uid")
                .build()
        val typeFilter: StringFilterConnector<TrackedEntityTypeAttributeCollectionRepository> =
            mock()
        val typeAttributeRepository: TrackedEntityTypeAttributeCollectionRepository = mock()
        whenever(
            d2.trackedEntityModule().trackedEntityTypeAttributes().byTrackedEntityTypeUid(),
        ) doReturn typeFilter
        whenever(typeFilter.eq("tracked-entity-type-uid")) doReturn typeAttributeRepository
        whenever(typeAttributeRepository.blockingGet()) doReturn
            listOf(
                TrackedEntityTypeAttribute
                    .builder()
                    .trackedEntityType(ObjectWithUid.create("tracked-entity-type-uid"))
                    .trackedEntityAttribute(ObjectWithUid.create("attribute-1"))
                    .displayInList(true)
                    .searchable(true)
                    .build(),
                TrackedEntityTypeAttribute
                    .builder()
                    .trackedEntityType(ObjectWithUid.create("tracked-entity-type-uid"))
                    .displayInList(true)
                    .searchable(true)
                    .build(),
                TrackedEntityTypeAttribute
                    .builder()
                    .trackedEntityType(ObjectWithUid.create("tracked-entity-type-uid"))
                    .trackedEntityAttribute(ObjectWithUid.create("attribute-3"))
                    .displayInList(true)
                    .searchable(true)
                    .build(),
            )

        val result = repository.getTrackedEntityTypeAttributeUids("tei-uid")

        assertEquals(listOf("attribute-1", "attribute-3"), result)
    }

    @Test
    fun `getTrackedEntityTypeAttributeUids should return empty list when tracked entity instance is missing`() = runBlocking {
        whenever(
            d2
                .trackedEntityModule()
                .trackedEntityInstances()
                .uid("tei-uid")
                .blockingGet(),
        ) doReturn null

        val result = repository.getTrackedEntityTypeAttributeUids("tei-uid")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTrackedEntityAttributeValue should return stored attribute value`() = runBlocking {
        whenever(
            d2
                .trackedEntityModule()
                .trackedEntityAttributeValues()
                .value("attribute-uid", "tei-uid")
                .blockingGet(),
        ) doReturn
            TrackedEntityAttributeValue
                .builder()
                .trackedEntityInstance("tei-uid")
                .trackedEntityAttribute("attribute-uid")
                .value("subject-guid")
                .build()

        val result =
            repository.getTrackedEntityAttributeValue(
                teiUid = "tei-uid",
                attributeUid = "attribute-uid",
            )

        assertEquals("subject-guid", result)
    }

    @Test
    fun `saveTrackedEntityAttributeValue should write attribute value`() = runBlocking {
        val attributeValueRepository: TrackedEntityAttributeValueObjectRepository = mock()
        whenever(
            d2
                .trackedEntityModule()
                .trackedEntityAttributeValues()
                .value("attribute-uid", "tei-uid"),
        ) doReturn attributeValueRepository

        repository.saveTrackedEntityAttributeValue(
            teiUid = "tei-uid",
            attributeUid = "attribute-uid",
            value = "subject-guid",
        )

        verify(attributeValueRepository).blockingSet("subject-guid")
    }
}
