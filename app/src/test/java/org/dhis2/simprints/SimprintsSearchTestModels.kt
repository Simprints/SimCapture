package org.dhis2.simprints

import org.dhis2.tracker.input.model.TrackerInputType
import org.dhis2.tracker.search.model.DomainEnrollment
import org.dhis2.tracker.search.model.EnrollmentStatus
import org.dhis2.tracker.search.model.GeometryFeatureType
import org.dhis2.tracker.search.model.SyncState
import org.dhis2.tracker.search.model.TrackedEntitySearchItemAttributeDomain
import org.dhis2.tracker.search.model.TrackedEntitySearchItemResult
import org.dhis2.tracker.search.model.TrackedEntityTypeDomain
import org.dhis2.usescases.searchTrackEntity.SearchTeiModel

internal fun trackedEntitySearchResult(
    uid: String,
    header: String = uid,
    isOnline: Boolean = false,
    selectedEnrollment: DomainEnrollment? = null,
    attributeValues: List<TrackedEntitySearchItemAttributeDomain> = emptyList(),
): TrackedEntitySearchItemResult =
    TrackedEntitySearchItemResult(
        uid = uid,
        created = null,
        lastUpdated = null,
        createdAtClient = null,
        lastUpdatedAtClient = null,
        ownerOrgUnit = null,
        enrollmentOrgUnit = null,
        shouldDisplayOrgUnit = false,
        geometry = null,
        syncState = SyncState.SYNCED,
        aggregatedSyncState = SyncState.SYNCED,
        deleted = false,
        isOnline = isOnline,
        teTypeName = "Person",
        type =
            TrackedEntityTypeDomain(
                trackedEntityTypeAttributeDomains = emptyList(),
                featureType = GeometryFeatureType.NONE,
            ),
        header = header,
        overDueDate = null,
        selectedEnrollment = selectedEnrollment,
        profilePicture = null,
        enrolledPrograms = null,
        enrollments = null,
        relationships = null,
        defaultTypeIcon = null,
        attributeValues = attributeValues,
    )

internal fun trackedEntityAttributeValue(
    attribute: String,
    value: String?,
): TrackedEntitySearchItemAttributeDomain =
    TrackedEntitySearchItemAttributeDomain(
        attribute = attribute,
        displayName = attribute,
        displayFormName = attribute,
        value = value,
        valueType = TrackerInputType.TEXT,
        displayInList = true,
        optionSet = null,
    )

internal fun domainEnrollment(
    uid: String,
    programUid: String?,
    teiUid: String,
): DomainEnrollment =
    DomainEnrollment(
        uid = uid,
        orgUnit = "orgUnit",
        program = programUid,
        enrollmentDate = null,
        incidentDate = null,
        completedDate = null,
        followUp = false,
        status = EnrollmentStatus.ACTIVE,
        trackedEntityInstance = teiUid,
    )

internal fun searchTeiModel(
    teiUid: String,
    header: String = teiUid,
    isOnline: Boolean = false,
    selectedEnrollment: DomainEnrollment? = null,
): SearchTeiModel =
    SearchTeiModel().apply {
        tei =
            trackedEntitySearchResult(
                uid = teiUid,
                header = header,
                isOnline = isOnline,
                selectedEnrollment = selectedEnrollment,
            )
    }
