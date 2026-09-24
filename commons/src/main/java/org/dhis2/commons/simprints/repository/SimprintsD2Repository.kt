package org.dhis2.commons.simprints.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhis2.commons.simprints.ramp.repository.RampDatastoreRepository
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.common.ValueType

class SimprintsD2Repository @JvmOverloads constructor(
    private val d2: D2,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(
        IO_PARALLEL_THREADS
    ),
    private val rampDatastoreRepository: RampDatastoreRepository = RampDatastoreRepository(d2),
) {
    data class EnrollmentContext(
        val teiUid: String,
        val programUid: String?,
        val orgUnitUid: String?,
    )

    suspend fun getEnrollmentContext(enrollmentUid: String): EnrollmentContext? = onIo {
        d2
            .enrollmentModule()
            .enrollments()
            .uid(enrollmentUid)
            .blockingGet()
            ?.let { enrollment ->
                EnrollmentContext(
                    teiUid = enrollment.trackedEntityInstance() ?: return@onIo null,
                    programUid = enrollment.program(),
                    orgUnitUid = enrollment.organisationUnit(),
                )
            }
    }

    suspend fun getProgramAttributeUids(programUid: String): List<String> = onIo {
        d2
            .programModule()
            .programTrackedEntityAttributes()
            .byProgram()
            .eq(programUid)
            .blockingGet()
            .mapNotNull { it.trackedEntityAttribute()?.uid() }
    }

    suspend fun getTrackedEntityTypeAttributeUids(teiUid: String): List<String> = onIo {
        d2
            .trackedEntityModule()
            .trackedEntityInstances()
            .uid(teiUid)
            .blockingGet()
            ?.trackedEntityType()
            ?.let { trackedEntityTypeUid ->
                d2
                    .trackedEntityModule()
                    .trackedEntityTypeAttributes()
                    .byTrackedEntityTypeUid()
                    .eq(trackedEntityTypeUid)
                    .blockingGet()
                    .mapNotNull { it.trackedEntityAttribute()?.uid() }
            } ?: emptyList()
    }

    suspend fun getTrackedEntityAttributeValue(
        teiUid: String,
        attributeUid: String,
    ): String? = onIo {
        d2
            .trackedEntityModule()
            .trackedEntityAttributeValues()
            .value(attributeUid, teiUid)
            .blockingGet()
            ?.value()
    }

    suspend fun saveTrackedEntityAttributeValue(
        teiUid: String,
        attributeUid: String,
        value: String,
    ) = onIo {
        d2
            .trackedEntityModule()
            .trackedEntityAttributeValues()
            .value(attributeUid, teiUid)
            .blockingSet(value)
    }

    suspend fun saveExternalCredential(
        teiUid: String,
        programUid: String?,
        biometricAttributeUid: String?,
        externalCredentialValue: String,
    ): String? =
        onIo {
            blockingSaveExternalCredential(teiUid, programUid, biometricAttributeUid, externalCredentialValue)
        }

    suspend fun saveEnrollmentExternalCredential(
        enrollmentUid: String,
        biometricAttributeUid: String,
        externalCredentialValue: String,
    ): String? =
        onIo {
            blockingSaveEnrollmentExternalCredential(enrollmentUid, biometricAttributeUid, externalCredentialValue)
        }

    fun blockingSaveEnrollmentExternalCredential(
        enrollmentUid: String,
        biometricAttributeUid: String,
        externalCredentialValue: String,
    ): String? {
        val enrollment =
            d2
                .enrollmentModule()
                .enrollments()
                .uid(enrollmentUid)
                .blockingGet() ?: return null
        return blockingSaveExternalCredential(
            enrollment.trackedEntityInstance() ?: return null,
            enrollment.program(),
            biometricAttributeUid,
            externalCredentialValue,
        )
    }

    private fun blockingSaveExternalCredential(
        teiUid: String,
        programUid: String?,
        biometricAttributeUid: String?,
        externalCredentialValue: String,
    ): String? {
        if (externalCredentialValue.isBlank()) return null
        val normalizedProgramUid = programUid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val attributeUid = rampDatastoreRepository.externalCredentialAttributeId(normalizedProgramUid) ?: return null
        if (attributeUid == biometricAttributeUid ||
            d2
                .programModule()
                .programTrackedEntityAttributes()
                .byProgram()
                .eq(normalizedProgramUid)
                .blockingGet()
                .none { it.trackedEntityAttribute()?.uid() == attributeUid }
        ) {
            return null
        }

        val attribute =
            d2
                .trackedEntityModule()
                .trackedEntityAttributes()
                .uid(attributeUid)
                .blockingGet()
                ?: return null
        if (attribute.valueType() != ValueType.TEXT ||
            attribute.generated() == true ||
            attribute.optionSet() != null
        ) {
            return null
        }

        val attributeValue =
            d2
                .trackedEntityModule()
                .trackedEntityAttributeValues()
                .value(attributeUid, teiUid)
        if (attributeValue.blockingGet()?.value() != externalCredentialValue) {
            attributeValue.blockingSet(externalCredentialValue)
        }
        return attributeUid
    }

    private suspend fun <T> onIo(block: () -> T): T = withContext(ioDispatcher) { block() }

    private companion object {
        private const val IO_PARALLEL_THREADS = 4
    }
}
