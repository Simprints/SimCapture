package org.dhis2.commons.simprints.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.dhis2.commons.Constants
import org.dhis2.commons.simprints.SimprintsBiometricSearchResult
import org.dhis2.commons.simprints.SimprintsBiometricsAction
import org.dhis2.commons.simprints.SimprintsBiometricsState
import org.dhis2.commons.simprints.repository.datastores.BiometricsResultSuccessRepository
import org.dhis2.commons.simprints.repository.datastores.BiometricsResultTimestampRepository
import org.dhis2.commons.simprints.repository.datastores.SimprintsBeneficiaryGuidRepository
import org.dhis2.commons.simprints.repository.datastores.SimprintsBiometricsProgressRepository
import org.dhis2.commons.simprints.repository.datastores.SimprintsModuleIdRepository
import org.dhis2.commons.simprints.repository.datastores.SimprintsProjectBiometricLockingTimeoutRepository
import org.dhis2.commons.simprints.repository.datastores.SimprintsProjectIdRepository
import org.dhis2.commons.simprints.repository.datastores.SimprintsProjectMatchThresholdRepository
import org.dhis2.commons.simprints.repository.datastores.UserIdRepository
import org.dhis2.commons.simprints.repository.flows.SimprintsBiometricEnrollmentRepository
import org.dhis2.commons.simprints.repository.flows.SimprintsBiometricIdentificationRepository
import org.dhis2.commons.simprints.repository.flows.SimprintsBiometricVerificationRepository
import org.hisp.dhis.android.core.D2

/**
 * Simprints biometrics actions (via separate Simprints ID app) and representation of
 * reactive Simprints-related data (from multiple sources) as unified source of truth.
 */
class SimprintsBiometricsRepository(
    private val d2: D2,
    coroutineScope: CoroutineScope,
    private val guidRepository: SimprintsBeneficiaryGuidRepository,
    private val moduleIdRepository: SimprintsModuleIdRepository,
    private val lockingTimeoutRepository: SimprintsProjectBiometricLockingTimeoutRepository,
    private val projectIdRepository: SimprintsProjectIdRepository,
    private val thresholdRepository: SimprintsProjectMatchThresholdRepository,
    private val userIdRepository: UserIdRepository,
    private val progressRepository: SimprintsBiometricsProgressRepository,
    private val resultTimestampRepository: BiometricsResultTimestampRepository,
    private val resultSuccessRepository: BiometricsResultSuccessRepository,
    private val enrollmentRepository: SimprintsBiometricEnrollmentRepository,
    private val verificationRepository: SimprintsBiometricVerificationRepository,
    private val identificationRepository: SimprintsBiometricIdentificationRepository,
) {

    // Simprints biometrics state for the currently accessed TEI as a beneficiary (if defined),
    // with best-effort data completeness based on what the data sources for its fields provide.
    private val biometricsStateFlow: MutableStateFlow<SimprintsBiometricsState> =
        MutableStateFlow(SimprintsBiometricsState())

    // Biometric search result events as the results of Simprints identification (one-o-many).
    private val biometricSearchResultFlow: MutableSharedFlow<SimprintsBiometricSearchResult> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        coroutineScope.launch {
            enrollmentRepository.getEnrollmentResultFlow()
                .collect { (simprintsGuid, biometricsResultSuccess) ->
                    val (teiUid, programUid) = progressRepository.getSimprintsBiometricsFinished()
                    val biometricsResultTimestamp = System.currentTimeMillis()
                    if (teiUid != null && simprintsGuid != null) {
                        guidRepository.setSimprintsGuid(teiUid, simprintsGuid)
                        resultTimestampRepository
                            .setResultTimestamp(teiUid, timestamp = biometricsResultTimestamp)
                        resultSuccessRepository
                            .setResultSuccess(teiUid, success = biometricsResultSuccess)
                    }
                    if (teiUid == biometricsStateFlow.value.teiUid) {
                        biometricsStateFlow.value =
                            biometricsStateFlow.value.copy(
                                simprintsGuid = simprintsGuid,
                                programUid = programUid,
                                lastBiometricsResultTimestamp = biometricsResultTimestamp,
                                lastBiometricsResultSuccess = biometricsResultSuccess,
                            )
                    }
                }
        }
        coroutineScope.launch {
            verificationRepository.getVerificationResultFlow()
                .collect { (simprintsMatchConfidenceScore) ->
                    val (teiUid, programUid) = progressRepository.getSimprintsBiometricsFinished()
                    val biometricsResultTimestamp = System.currentTimeMillis()
                    val biometricsResultSuccess = if (teiUid != null) {
                        val projectMatchThreshold =
                            thresholdRepository.getSimprintsMatchThreshold(programUid)
                        projectMatchThreshold != null &&
                            simprintsMatchConfidenceScore != null &&
                            simprintsMatchConfidenceScore >= projectMatchThreshold
                    } else {
                        false
                    }
                    resultTimestampRepository
                        .setResultTimestamp(teiUid, timestamp = biometricsResultTimestamp)
                    resultSuccessRepository
                        .setResultSuccess(teiUid, success = biometricsResultSuccess)
                    if (teiUid == biometricsStateFlow.value.teiUid) {
                        biometricsStateFlow.value =
                            biometricsStateFlow.value.copy(
                                lastBiometricsResultTimestamp = biometricsResultTimestamp,
                                lastBiometricsResultSuccess = biometricsResultSuccess,
                            )
                    }
                }
        }
        coroutineScope.launch {
            identificationRepository.getIdentificationResultFlow()
                .collect { identifications ->
                    val (_, programUid) = progressRepository.getSimprintsBiometricsFinished()
                    val biometricsResultTimestamp = System.currentTimeMillis()
                    val projectMatchThreshold =
                        thresholdRepository.getSimprintsMatchThreshold(programUid) ?: 0
                    identifications.filter { (_, simprintsMatchConfidenceScore) ->
                        simprintsMatchConfidenceScore >= projectMatchThreshold
                    }.sortedByDescending { (_, simprintsMatchConfidenceScore) ->
                        simprintsMatchConfidenceScore
                    }.map { (simprintsGuid, _) ->
                        simprintsGuid
                    }.run(guidRepository::getExistingGuidsPairedToTeiUids).map { (guid, teiUid) ->
                        resultTimestampRepository
                            .setResultTimestamp(teiUid, timestamp = biometricsResultTimestamp)
                        resultSuccessRepository
                            .setResultSuccess(teiUid, success = true)
                        guid
                    }.run {
                        SimprintsBiometricSearchResult(
                            timestamp = biometricsResultTimestamp,
                            teiUids = this,
                        )
                    }.run(biometricSearchResultFlow::tryEmit)
                }
        }
    }

    fun getSimprintsGuidAttributeUids(): List<String> =
        guidRepository.getSimprintsGuidAttributeUids()

    fun getSimprintsBiometricsStateFlow(
        teiUid: String? = null,
        programUid: String? = null,
        enrollmentUid: String? = null,
    ): StateFlow<SimprintsBiometricsState> =
        biometricsStateFlow.apply {
            if (teiUid != null && teiUid != value.teiUid ||
                programUid != null && programUid != value.programUid ||
                enrollmentUid != null && enrollmentUid != value.programUid
            ) {
                value = getTeiState(teiUid, programUid, enrollmentUid)
            }
        }

    fun getBiometricSearchResultFlow(programUid: String?): Flow<List<String>> =
        biometricSearchResultFlow.map { result ->
            guidRepository.getTeiUidsInProgram(result.teiUids, programUid)
        }

    fun isBiometricIdentificationAvailable(programUid: String?): Boolean =
        projectIdRepository.getSimprintsProjectId(programUid) != null &&
            thresholdRepository.getSimprintsMatchThreshold(programUid) != null &&
            userIdRepository.getUserId() != null &&
            getSimprintsGuidAttributeUids().isNotEmpty()

    fun dispatchSimprintsAction(action: SimprintsBiometricsAction) {
        with(biometricsStateFlow.value) {
            progressRepository.setSimprintsBiometricsInProgress(teiUid, programUid)
        }
        with(biometricsStateFlow.value) {
            when {
                simprintsProjectId == null -> {} // no-op
                userId == null -> {} // no-op
                // simprintsModuleId can be null

                action.isOneToMany -> {
                    identificationRepository.launchIdentify(
                        projectId = simprintsProjectId,
                        userId = userId,
                        moduleId = simprintsModuleId ?: "default",
                    )
                }

                simprintsGuid == null -> {
                    enrollmentRepository.launchEnroll(
                        projectId = simprintsProjectId,
                        userId = userId,
                        moduleId = simprintsModuleId ?: "default",
                    )
                }

                else -> {
                    verificationRepository.launchVerify(
                        projectId = simprintsProjectId,
                        userId = userId,
                        moduleId = simprintsModuleId ?: "default",
                        guid = simprintsGuid,
                    )
                }
            }
        }
    }

    fun downSyncSimprintsConfigs() {
        d2.dataStoreModule().dataStoreDownloader()
            .byNamespace().eq(Constants.SIMPRINTS_NAMESPACE).blockingDownload()
        moduleIdRepository.clearCache()
        lockingTimeoutRepository.clearCache()
        projectIdRepository.clearCache()
        thresholdRepository.clearCache()
        biometricsStateFlow.value = biometricsStateFlow.value.run {
            getTeiState(teiUid, programUid, enrollmentUid)
        }
    }

    private fun getTeiState(
        teiUid: String?,
        programUid: String?,
        enrollmentUid: String?,
    ): SimprintsBiometricsState {
        val orgUnitUid =
            d2.trackedEntityModule().trackedEntityInstances().byUid()
                .eq(teiUid)
                .blockingGet().firstOrNull()?.organisationUnit()
        return SimprintsBiometricsState(
            teiUid = teiUid,
            simprintsGuid = teiUid?.let { guidRepository.getSimprintsGuid(teiUid) },
            programUid = programUid,
            enrollmentUid = enrollmentUid,
            simprintsProjectId = projectIdRepository.getSimprintsProjectId(programUid),
            orgUnitUid = orgUnitUid,
            simprintsModuleId = moduleIdRepository.getSimprintsModuleId(orgUnitUid),
            userId = userIdRepository.getUserId(),
            simprintsMatchThreshold = thresholdRepository.getSimprintsMatchThreshold(programUid),
            biometricLockingTimeoutMinutes = lockingTimeoutRepository.getTimeoutMinutes(programUid),
            lastBiometricsResultTimestamp = resultTimestampRepository.getResultTimestamp(teiUid),
            lastBiometricsResultSuccess = resultSuccessRepository.getResultSuccess(teiUid),
        )
    }
}
