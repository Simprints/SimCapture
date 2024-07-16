package com.simprints.simprints.di

import com.google.gson.Gson
import com.simprints.simprints.repository.SimprintsBiometricsRepository
import com.simprints.simprints.repository.datastores.BiometricsResultSuccessRepository
import com.simprints.simprints.repository.datastores.BiometricsResultTimestampRepository
import com.simprints.simprints.repository.datastores.SimprintsBeneficiaryGuidRepository
import com.simprints.simprints.repository.datastores.SimprintsBeneficiarySubjectActionRepository
import com.simprints.simprints.repository.datastores.SimprintsBiometricsProgressRepository
import com.simprints.simprints.repository.datastores.SimprintsModuleIdRepository
import com.simprints.simprints.repository.datastores.SimprintsProjectBiometricLockingTimeoutRepository
import com.simprints.simprints.repository.datastores.SimprintsProjectIdRepository
import com.simprints.simprints.repository.datastores.SimprintsProjectMatchThresholdRepository
import com.simprints.simprints.repository.datastores.UserIdRepository
import com.simprints.simprints.repository.flows.SimprintsBiometricEnrollmentRepository
import com.simprints.simprints.repository.flows.SimprintsBiometricIdentificationRepository
import com.simprints.simprints.repository.flows.SimprintsBiometricVerificationRepository
import com.simprints.simprints.touchpoints.app.usescases.teidashboard.SimprintsTEIDataViewModelFactory
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import org.hisp.dhis.android.core.D2Manager
import javax.inject.Singleton

@Module
class SimprintsBiometricsDiModule {
    @OptIn(DelicateCoroutinesApi::class)
    @Provides
    @Singleton
    fun simprintsBiometricsRepository(
        guidRepository: SimprintsBeneficiaryGuidRepository,
        subjectActionRepository: SimprintsBeneficiarySubjectActionRepository,
        moduleIdRepository: SimprintsModuleIdRepository,
        biometricLockabilityRepository: SimprintsProjectBiometricLockingTimeoutRepository,
        projectIdRepository: SimprintsProjectIdRepository,
        projectMatchThresholdRepository: SimprintsProjectMatchThresholdRepository,
        userIdRepository: UserIdRepository,
        progressRepository: SimprintsBiometricsProgressRepository,
        resultTimestampRepository: BiometricsResultTimestampRepository,
        resultSuccessRepository: BiometricsResultSuccessRepository,
        enrollmentRepository: SimprintsBiometricEnrollmentRepository,
        verificationRepository: SimprintsBiometricVerificationRepository,
        identificationRepository: SimprintsBiometricIdentificationRepository,
    ): SimprintsBiometricsRepository {
        return SimprintsBiometricsRepository(
            d2 = D2Manager.getD2(),
            coroutineScope = GlobalScope,
            guidRepository,
            subjectActionRepository,
            moduleIdRepository,
            biometricLockabilityRepository,
            projectIdRepository,
            projectMatchThresholdRepository,
            userIdRepository,
            progressRepository,
            resultTimestampRepository,
            resultSuccessRepository,
            enrollmentRepository,
            verificationRepository,
            identificationRepository,
        )
    }

    @Provides
    @Singleton
    fun guidRepository(): SimprintsBeneficiaryGuidRepository {
        return SimprintsBeneficiaryGuidRepository(d2 = D2Manager.getD2())
    }

    @Provides
    @Singleton
    fun subjectActionRepository(): SimprintsBeneficiarySubjectActionRepository {
        return SimprintsBeneficiarySubjectActionRepository(d2 = D2Manager.getD2())
    }

    @Provides
    @Singleton
    fun moduleIdRepository(gson: Gson): SimprintsModuleIdRepository {
        return SimprintsModuleIdRepository(d2 = D2Manager.getD2(), gson)
    }

    @Provides
    @Singleton
    fun projectBiometricLockabilityRepository(
        gson: Gson,
    ): SimprintsProjectBiometricLockingTimeoutRepository {
        return SimprintsProjectBiometricLockingTimeoutRepository(d2 = D2Manager.getD2(), gson)
    }

    @Provides
    @Singleton
    fun projectIdRepository(gson: Gson): SimprintsProjectIdRepository {
        return SimprintsProjectIdRepository(d2 = D2Manager.getD2(), gson)
    }

    @Provides
    @Singleton
    fun projectMatchThresholdRepository(gson: Gson): SimprintsProjectMatchThresholdRepository {
        return SimprintsProjectMatchThresholdRepository(d2 = D2Manager.getD2(), gson)
    }

    @Provides
    @Singleton
    fun userIdRepository(): UserIdRepository {
        return UserIdRepository(d2 = D2Manager.getD2())
    }

    @Provides
    @Singleton
    fun progressRepository(gson: Gson): SimprintsBiometricsProgressRepository {
        return SimprintsBiometricsProgressRepository(d2 = D2Manager.getD2(), gson)
    }

    @Provides
    @Singleton
    fun resultTimestampRepository(): BiometricsResultTimestampRepository {
        return BiometricsResultTimestampRepository(d2 = D2Manager.getD2())
    }

    @Provides
    @Singleton
    fun resultSuccessRepository(): BiometricsResultSuccessRepository {
        return BiometricsResultSuccessRepository(d2 = D2Manager.getD2())
    }

    @Provides
    @Singleton
    fun simprintsTeiDataViewModelFactory(
        simprintsBiometricsRepository: SimprintsBiometricsRepository,
    ): SimprintsTEIDataViewModelFactory {
        return SimprintsTEIDataViewModelFactory(simprintsBiometricsRepository)
    }
}
