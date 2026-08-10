package org.dhis2.usescases.teiDashboard.teiProgramList

import io.reactivex.Single
import org.dhis2.R
import org.dhis2.commons.date.DateUtils
import org.dhis2.commons.resources.MetadataIconProvider
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.simprints.ramp.model.DetailedEnrollmentListingSettings
import org.dhis2.simprints.ramp.data.DetailedEnrollmentRepository
import org.dhis2.simprints.ramp.model.DetailedEnrollment
import org.dhis2.usescases.main.program.ProgramViewModelMapper
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.filters.internal.BooleanFilterConnector
import org.hisp.dhis.android.core.arch.repositories.filters.internal.EnumFilterConnector
import org.hisp.dhis.android.core.arch.repositories.filters.internal.StringFilterConnector
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.ObjectStyle
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.enrollment.EnrollmentCollectionRepository
import org.hisp.dhis.android.core.enrollment.EnrollmentCreateProjection
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitCollectionRepository
import org.hisp.dhis.android.core.program.Program
import org.hisp.dhis.android.core.program.ProgramCollectionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

class TeiProgramListRepositoryImplTest {
    private lateinit var teiProgramRepository: TeiProgramListRepository
    private val d2: D2 = Mockito.mock(D2::class.java, Mockito.RETURNS_DEEP_STUBS)
    private val programViewModelMapper: ProgramViewModelMapper = mock()
    private val metadataIconProvider: MetadataIconProvider = mock()
    private val detailedEnrollmentRepository: DetailedEnrollmentRepository = mock()
    private val resourceManager: ResourceManager = mock()
    private val listingSettings = DetailedEnrollmentListingSettings(emptySet(), emptySet(), emptySet())

    @Before
    fun setUp() {
        teiProgramRepository = createRepository()
        whenever(resourceManager.getString(R.string.simprints_ramp_admitted)) doReturn "Admitted"
        whenever(resourceManager.getString(R.string.simprints_ramp_discharged)) doReturn "Discharged"
        whenever(resourceManager.getString(R.string.simprints_ramp_outcome)) doReturn "Outcome"
        whenever(resourceManager.getString(R.string.simprints_ramp_site)) doReturn "Site"
    }

    @Test
    fun `should format available simprints ramp configured details and omit unavailable details`() {
        val fullEnrollment = enrollment(FULL_ENROLLMENT_UID)
        val partialEnrollment = enrollment(PARTIAL_ENROLLMENT_UID)
        val enrollments = listOf(fullEnrollment, partialEnrollment)
        stubActiveEnrollments(enrollments)
        whenever(detailedEnrollmentRepository.get(enrollments, listingSettings)) doReturn
            mapOf(
                FULL_ENROLLMENT_UID to
                    DetailedEnrollment(
                        admitted = date(ADMITTED_DATE),
                        discharge = date(DISCHARGE_DATE),
                        outcome = OUTCOME_VALUE,
                        site = ORGANISATION_UNIT_NAME,
                    ),
                PARTIAL_ENROLLMENT_UID to
                    DetailedEnrollment(
                        admitted = null,
                        discharge = date(DISCHARGE_DATE),
                        outcome = " ",
                        site = ORGANISATION_UNIT_NAME,
                    ),
            )
        teiProgramRepository = createRepository(listingSettings)

        val result = teiProgramRepository.activeEnrollments(TEI_UID).blockingFirst()

        assertEquals(
            listOf(
                "Admitted: $ADMITTED_DATE, Discharged: $DISCHARGE_DATE, " +
                    "Outcome: $OUTCOME_VALUE, Site: $ORGANISATION_UNIT_NAME",
                "Discharged: $DISCHARGE_DATE, Site: $ORGANISATION_UNIT_NAME",
            ),
            result.map { it.enrollmentDetails },
        )
    }

    @Test
    fun `should retain standard row outside simprints ramp configured program`() {
        val enrollment = enrollment(FULL_ENROLLMENT_UID)
        stubActiveEnrollments(listOf(enrollment))
        val organisationUnits =
            Mockito.mock(
                OrganisationUnitCollectionRepository::class.java,
                Mockito.RETURNS_DEEP_STUBS,
            )
        val organisationUnitUidFilter:
            StringFilterConnector<OrganisationUnitCollectionRepository> = mock()
        val organisationUnit =
            OrganisationUnit
                .builder()
                .uid(ORGANISATION_UNIT_UID)
                .displayName(ORGANISATION_UNIT_NAME)
                .build()
        whenever(d2.organisationUnitModule().organisationUnits()) doReturn organisationUnits
        whenever(organisationUnits.byUid()) doReturn organisationUnitUidFilter
        whenever(organisationUnitUidFilter.eq(ORGANISATION_UNIT_UID)) doReturn organisationUnits
        whenever(organisationUnits.one().blockingGet()) doReturn organisationUnit

        val result = teiProgramRepository.activeEnrollments(TEI_UID).blockingFirst().single()

        assertNull(result.enrollmentDetails)
        assertEquals(ORGANISATION_UNIT_NAME, result.orgUnitName)
        assertEquals(ENROLLMENT_DATE, result.enrollmentDate)
    }

    @Test
    fun `Should set incident date if program needs it`() {
        val testEnrollment =
            EnrollmentCreateProjection
                .builder()
                .organisationUnit("orgUnitUid")
                .program("programUid")
                .trackedEntityInstance("teiUid")
                .build()

        whenever(
            d2.enrollmentModule().enrollments().add(
                testEnrollment,
            ),
        ) doReturn Single.just("enrollmentUid")

        whenever(
            d2.enrollmentModule().enrollments().uid("enrollmentUid"),
        ) doReturn mock()

        whenever(
            d2
                .programModule()
                .programs()
                .uid("programUid")
                .blockingGet(),
        ) doReturn
            Program
                .builder()
                .uid("programUid")
                .displayIncidentDate(true)
                .categoryCombo(ObjectWithUid.create("categoryComboUid"))
                .enrollmentCategoryCombo(ObjectWithUid.create("categoryComboUid"))
                .build()

        whenever(
            d2
                .enrollmentModule()
                .enrollments()
                .uid("enrollmentUid")
                .blockingGet(),
        ) doReturn
            Enrollment
                .builder()
                .uid("enrollmentUid")
                .attributeOptionCombo("attributeOptionComboUid")
                .build()

        val testObservable =
            teiProgramRepository
                .saveToEnroll(
                    "orgUnitUid",
                    "programUid",
                    "teiUid",
                    Date(),
                ).test()

        testObservable
            .assertNoErrors()
            .assertValueCount(1)
            .assertValue { it == "enrollmentUid" }

        verify(d2.enrollmentModule().enrollments().uid("enrollmentUid"), times(1)).setIncidentDate(
            DateUtils.getInstance().today,
        )
    }

    @Test
    fun `Should not set incident date if program doesn't need it`() {
        val testEnrollment =
            EnrollmentCreateProjection
                .builder()
                .organisationUnit("orgUnitUid")
                .program("programUid")
                .trackedEntityInstance("teiUid")
                .build()

        whenever(
            d2.enrollmentModule().enrollments().add(
                testEnrollment,
            ),
        ) doReturn Single.just("enrollmentUid")

        whenever(
            d2.enrollmentModule().enrollments().uid("enrollmentUid"),
        ) doReturn mock()

        whenever(
            d2
                .programModule()
                .programs()
                .uid("programUid")
                .blockingGet(),
        ) doReturn
            Program
                .builder()
                .uid("programUid")
                .displayIncidentDate(false)
                .categoryCombo(ObjectWithUid.create("categoryComboUid"))
                .enrollmentCategoryCombo(ObjectWithUid.create("categoryComboUid"))
                .build()

        whenever(
            d2
                .enrollmentModule()
                .enrollments()
                .uid("enrollmentUid")
                .blockingGet(),
        ) doReturn
            Enrollment
                .builder()
                .uid("enrollmentUid")
                .attributeOptionCombo("attributeOptionComboUid")
                .build()

        val testObservable =
            teiProgramRepository
                .saveToEnroll(
                    "orgUnitUid",
                    "programUid",
                    "teiUid",
                    Date(),
                ).test()

        testObservable
            .assertNoErrors()
            .assertValueCount(1)
            .assertValue { it == "enrollmentUid" }

        verify(d2.enrollmentModule().enrollments().uid("enrollmentUid"), times(0)).setIncidentDate(
            DateUtils.getInstance().today,
        )
    }

    private fun enrollment(uid: String): Enrollment =
        mock {
            on { uid() } doReturn uid
            on { program() } doReturn PROGRAM_UID
            on { organisationUnit() } doReturn ORGANISATION_UNIT_UID
            on { enrollmentDate() } doReturn date(ENROLLMENT_DATE)
            on { followUp() } doReturn false
        }

    private fun stubActiveEnrollments(enrollments: List<Enrollment>) {
        val enrollmentRepository: EnrollmentCollectionRepository = mock()
        val trackedEntityFilter: StringFilterConnector<EnrollmentCollectionRepository> = mock()
        val statusFilter: EnumFilterConnector<EnrollmentCollectionRepository, EnrollmentStatus> = mock()
        val deletedFilter: BooleanFilterConnector<EnrollmentCollectionRepository> = mock()
        whenever(d2.enrollmentModule().enrollments()) doReturn enrollmentRepository
        whenever(enrollmentRepository.byTrackedEntityInstance()) doReturn trackedEntityFilter
        whenever(trackedEntityFilter.eq(TEI_UID)) doReturn enrollmentRepository
        whenever(enrollmentRepository.byStatus()) doReturn statusFilter
        whenever(statusFilter.eq(EnrollmentStatus.ACTIVE)) doReturn enrollmentRepository
        whenever(enrollmentRepository.byDeleted()) doReturn deletedFilter
        whenever(deletedFilter.eq(false)) doReturn enrollmentRepository
        whenever(enrollmentRepository.blockingGet()) doReturn enrollments

        val programs =
            Mockito.mock(ProgramCollectionRepository::class.java, Mockito.RETURNS_DEEP_STUBS)
        val programUidFilter: StringFilterConnector<ProgramCollectionRepository> = mock()
        val program: Program = mock {
            on { uid() } doReturn PROGRAM_UID
            on { displayName() } doReturn PROGRAM_NAME
            on { style() } doReturn ObjectStyle.builder().build()
        }
        whenever(d2.programModule().programs()) doReturn programs
        whenever(programs.byUid()) doReturn programUidFilter
        whenever(programUidFilter.eq(PROGRAM_UID)) doReturn programs
        whenever(programs.one().blockingGet()) doReturn program
    }

    private fun date(value: String): Date = DateUtils.uiDateFormat().parse(value)!!

    private fun createRepository(settings: DetailedEnrollmentListingSettings? = null) =
        TeiProgramListRepositoryImpl(
            d2,
            programViewModelMapper,
            metadataIconProvider,
            detailedEnrollmentRepository,
            { settings },
            resourceManager,
        )

    private companion object {
        const val TEI_UID = "trackedEntity"
        const val FULL_ENROLLMENT_UID = "fullEnrollment"
        const val PARTIAL_ENROLLMENT_UID = "partialEnrollment"
        const val PROGRAM_UID = "program"
        const val ORGANISATION_UNIT_UID = "organisationUnit"
        const val PROGRAM_NAME = "Program"
        const val ORGANISATION_UNIT_NAME = "Organisation unit"
        const val OUTCOME_VALUE = "Outcome value"
        const val ADMITTED_DATE = "01/01/2020"
        const val DISCHARGE_DATE = "02/01/2020"
        const val ENROLLMENT_DATE = ADMITTED_DATE
    }
}
