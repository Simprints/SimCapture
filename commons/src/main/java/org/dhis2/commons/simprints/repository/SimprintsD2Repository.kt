package org.dhis2.commons.simprints.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hisp.dhis.android.core.D2

class SimprintsD2Repository @JvmOverloads constructor(
    private val d2: D2,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(
        IO_PARALLEL_THREADS
    ),
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

    private suspend fun <T> onIo(block: () -> T): T = withContext(ioDispatcher) { block() }

    private companion object {
        private const val IO_PARALLEL_THREADS = 4
    }
}
