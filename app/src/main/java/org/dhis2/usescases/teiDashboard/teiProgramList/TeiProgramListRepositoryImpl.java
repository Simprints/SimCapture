package org.dhis2.usescases.teiDashboard.teiProgramList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.dhis2.R;
import org.dhis2.commons.date.DateUtils;
import org.dhis2.commons.resources.ResourceManager;
import org.dhis2.commons.resources.MetadataIconProvider;
import org.dhis2.commons.simprints.ramp.model.DetailedEnrollmentListingSettings;
import org.dhis2.simprints.ramp.data.DetailedEnrollmentRepository;
import org.dhis2.simprints.ramp.model.DetailedEnrollment;
import org.dhis2.usescases.main.program.ProgramDownloadState;
import org.dhis2.usescases.main.program.ProgramUiModel;
import org.dhis2.usescases.main.program.ProgramViewModelMapper;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.EnrollmentCreateProjection;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.reactivex.Flowable;
import io.reactivex.Observable;

public class TeiProgramListRepositoryImpl implements TeiProgramListRepository {

    private final D2 d2;
    private final ProgramViewModelMapper programViewModelMapper;
    private final MetadataIconProvider metadataIconProvider;
    private final DetailedEnrollmentRepository detailedEnrollmentRepository;
    private final Supplier<DetailedEnrollmentListingSettings> detailedEnrollmentListingSettings;
    private final ResourceManager resourceManager;

    TeiProgramListRepositoryImpl(
            D2 d2,
            ProgramViewModelMapper programViewModelMapper,
            MetadataIconProvider metadataIconProvider,
            DetailedEnrollmentRepository detailedEnrollmentRepository,
            Supplier<DetailedEnrollmentListingSettings> detailedEnrollmentListingSettings,
            ResourceManager resourceManager
    ) {
        this.d2 = d2;
        this.programViewModelMapper = programViewModelMapper;
        this.metadataIconProvider = metadataIconProvider;
        this.detailedEnrollmentRepository = detailedEnrollmentRepository;
        this.detailedEnrollmentListingSettings = detailedEnrollmentListingSettings;
        this.resourceManager = resourceManager;
    }

    @NonNull
    @Override
    public Observable<List<EnrollmentViewModel>> activeEnrollments(String trackedEntityId) {
        return Observable.fromCallable(() ->
                        d2.enrollmentModule().enrollments()
                                .byTrackedEntityInstance().eq(trackedEntityId)
                                .byStatus().eq(EnrollmentStatus.ACTIVE)
                                .byDeleted().eq(false).blockingGet())
                .flatMap(this::mapEnrollments);
    }

    @NonNull
    @Override
    public Observable<List<EnrollmentViewModel>> otherEnrollments(String trackedEntityId) {
        return Observable.fromCallable(() -> d2.enrollmentModule().enrollments().byTrackedEntityInstance().eq(trackedEntityId).byStatus().neq(EnrollmentStatus.ACTIVE).blockingGet())
                .flatMap(this::mapEnrollments);
    }

    private Observable<List<EnrollmentViewModel>> mapEnrollments(
            List<org.hisp.dhis.android.core.enrollment.Enrollment> enrollments
    ) {
        DetailedEnrollmentListingSettings listingSettings = detailedEnrollmentListingSettings.get();
        Map<String, DetailedEnrollment> detailedEnrollments =
                listingSettings == null ?
                        Collections.emptyMap() :
                        detailedEnrollmentRepository.get(
                                enrollments,
                                listingSettings
                        );

        return Observable.fromIterable(enrollments)
                .map(enrollment -> {
                    Program program = d2.programModule().programs().byUid().eq(enrollment.program()).one().blockingGet();
                    DetailedEnrollment detailedEnrollment = detailedEnrollments.get(enrollment.uid());
                    String orgUnitName;
                    if (detailedEnrollment != null) {
                        orgUnitName = detailedEnrollment.getSite() == null ? "" : detailedEnrollment.getSite();
                    } else {
                        OrganisationUnit orgUnit = d2.organisationUnitModule().organisationUnits().byUid().eq(enrollment.organisationUnit()).one().blockingGet();
                        orgUnitName = orgUnit.displayName();
                    }

                    return new EnrollmentViewModel(
                            enrollment.uid(),
                            DateUtils.getInstance().formatDate(enrollment.enrollmentDate()),
                            metadataIconProvider.invoke(program.style()),
                            program.displayName(),
                            orgUnitName,
                            enrollment.followUp() != null ? enrollment.followUp() : false,
                            program.uid(),
                            formatDetailedEnrollment(detailedEnrollment)
                    );
                })
                .toList()
                .toObservable();
    }

    @Nullable
    private String formatDetailedEnrollment(@Nullable DetailedEnrollment enrollment) {
        if (enrollment == null) {
            return null;
        }

        List<String> details = new ArrayList<>();
        if (enrollment.getAdmitted() != null) {
            details.add(
                    resourceManager.getString(R.string.simprints_ramp_admitted) + ": " +
                            DateUtils.getInstance().formatDate(enrollment.getAdmitted())
            );
        }
        if (enrollment.getDischarge() != null) {
            details.add(
                    resourceManager.getString(R.string.simprints_ramp_discharged) + ": " +
                            DateUtils.getInstance().formatDate(enrollment.getDischarge())
            );
        }
        if (enrollment.getOutcome() != null && !enrollment.getOutcome().trim().isEmpty()) {
            details.add(
                    resourceManager.getString(R.string.simprints_ramp_outcome) + ": " +
                            enrollment.getOutcome()
            );
        }
        if (enrollment.getSite() != null && !enrollment.getSite().trim().isEmpty()) {
            details.add(
                    resourceManager.getString(R.string.simprints_ramp_site) + ": " +
                            enrollment.getSite()
            );
        }

        return String.join(", ", details);
    }

    @NonNull
    @Override
    public Flowable<List<ProgramUiModel>> allPrograms(String trackedEntityId) {
        String trackedEntityType = d2.trackedEntityModule().trackedEntityInstances().byUid().eq(trackedEntityId).one().blockingGet().trackedEntityType();
        return Flowable.just(d2.organisationUnitModule().organisationUnits().byOrganisationUnitScope(OrganisationUnit.Scope.SCOPE_DATA_CAPTURE).blockingGet())
                .map(captureOrgUnits -> {
                    Iterator<OrganisationUnit> it = captureOrgUnits.iterator();
                    List<String> captureOrgUnitUids = new ArrayList();
                    while (it.hasNext()) {
                        OrganisationUnit ou = it.next();
                        captureOrgUnitUids.add(ou.uid());
                    }
                    return captureOrgUnitUids;
                })
                .flatMap(orgUnits -> Flowable.fromCallable(() -> d2.programModule().programs()
                        .byOrganisationUnitList(orgUnits)
                        .byTrackedEntityTypeUid().eq(trackedEntityType).blockingGet()))
                .flatMapIterable(programs -> programs)
                .map(program ->
                        programViewModelMapper.map(
                                program,
                                0,
                                "",
                                State.SYNCED,
                                false,
                                metadataIconProvider.invoke(program.style())
                        )
                )
                .toList()
                .toFlowable();
    }

    @NonNull
    @Override
    public Observable<List<Program>> alreadyEnrolledPrograms(String trackedEntityId) {
        return Observable.fromCallable(() ->
                        d2.enrollmentModule().enrollments()
                                .byTrackedEntityInstance().eq(trackedEntityId)
                                .byDeleted().eq(false).blockingGet())
                .flatMapIterable(enrollments -> enrollments)
                .map(enrollment -> d2.programModule().programs().byUid().eq(enrollment.program()).one().blockingGet())
                .toList()
                .toObservable();
    }

    @NonNull
    @Override
    public Observable<String> saveToEnroll(@NonNull String orgUnit, @NonNull String programUid, @NonNull String teiUid, Date enrollmentDate) {
        return d2.enrollmentModule().enrollments().add(
                        EnrollmentCreateProjection.builder()
                                .organisationUnit(orgUnit)
                                .program(programUid)
                                .trackedEntityInstance(teiUid)
                                .build())
                .map(enrollmentUid ->
                        d2.enrollmentModule().enrollments().uid(enrollmentUid))
                .map(enrollmentRepository -> {
                    if (d2.programModule().programs().uid(programUid).blockingGet().displayIncidentDate()) {
                        enrollmentRepository.setIncidentDate(DateUtils.getInstance().getToday());
                    }
                    enrollmentRepository.setEnrollmentDate(enrollmentDate);
                    enrollmentRepository.setFollowUp(false);
                    return enrollmentRepository.blockingGet().uid();
                }).toObservable();
    }

    @Override
    public Observable<List<OrganisationUnit>> getOrgUnits(String programUid) {
        if (programUid != null)
            return d2.organisationUnitModule().organisationUnits().byOrganisationUnitScope(OrganisationUnit.Scope.SCOPE_DATA_CAPTURE)
                    .byProgramUids(Collections.singletonList(programUid)).get().toObservable();
        else
            return d2.organisationUnitModule().organisationUnits().byOrganisationUnitScope(OrganisationUnit.Scope.SCOPE_DATA_CAPTURE).get().toObservable();
    }

    @Override
    public String getProgramColor(@NonNull String programUid) {
        Program program = d2.programModule().programs().byUid().eq(programUid).one().blockingGet();
        return program.style() != null ? program.style().color() : null;
    }

    @Override
    public Program getProgram(String programUid) {
        Program program = d2.programModule().programs().byUid().eq(programUid).one().blockingGet();
        return program;
    }

    @Override
    public ProgramUiModel updateProgramViewModel(ProgramUiModel programUiModel, ProgramDownloadState programDownloadState) {
        return programViewModelMapper.map(programUiModel, programDownloadState);
    }
}