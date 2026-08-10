package org.dhis2.simprints.ramp.data

import org.dhis2.bindings.userFriendlyValue
import org.dhis2.commons.simprints.ramp.model.DetailedEnrollmentListingSettings
import org.dhis2.simprints.ramp.model.DetailedEnrollment
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.event.Event
import java.util.Date

internal class DetailedEnrollmentRepository(
    private val d2: D2,
) {
    fun get(
        enrollments: List<Enrollment>,
        settings: DetailedEnrollmentListingSettings,
    ): Map<String, DetailedEnrollment> {
        val includedEnrollments = enrollments.filter { !it.uid().isNullOrBlank() }
        if (includedEnrollments.isEmpty()) return emptyMap()

        val siteNames = getSiteNames(includedEnrollments)
        val eventDetails = getEventDetails(includedEnrollments, settings)

        return includedEnrollments.associate { enrollment ->
            val enrollmentUid = enrollment.uid()
            val details = eventDetails[enrollmentUid]
            enrollmentUid to
                DetailedEnrollment(
                    admitted = details?.admitted,
                    discharge = details?.discharge,
                    outcome = details?.outcome,
                    site = siteNames[enrollment.organisationUnit()],
                )
        }
    }

    private fun getSiteNames(enrollments: List<Enrollment>): Map<String, String> {
        val orgUnitIds = enrollments.mapNotNull { it.organisationUnit() }.distinct()
        if (orgUnitIds.isEmpty()) return emptyMap()

        return d2
            .organisationUnitModule()
            .organisationUnits()
            .byUid()
            .`in`(orgUnitIds)
            .blockingGet()
            .mapNotNull { orgUnit ->
                val uid = orgUnit.uid()
                val name = orgUnit.displayName()
                if (uid.isNullOrBlank() || name.isNullOrBlank()) null else uid to name
            }.toMap()
    }

    private fun getEventDetails(
        enrollments: List<Enrollment>,
        settings: DetailedEnrollmentListingSettings,
    ): Map<String, EnrollmentEventDetails> {
        if (
            settings.admissionProgramStageIds.isEmpty() &&
            settings.dischargeProgramStageIds.isEmpty()
        ) {
            return emptyMap()
        }

        val enrollmentIds = enrollments.map { it.uid() }.distinct()
        if (enrollmentIds.isEmpty()) return emptyMap()

        return d2
            .eventModule()
            .events()
            .withTrackedEntityDataValues()
            .byEnrollmentUid()
            .`in`(enrollmentIds)
            .byDeleted()
            .isFalse
            .blockingGet()
            .groupBy { it.enrollment() }
            .mapNotNull { (enrollmentId, events) ->
                enrollmentId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { it to events.toEnrollmentEventDetails(settings) }
            }.toMap()
    }

    private fun List<Event>.toEnrollmentEventDetails(
        settings: DetailedEnrollmentListingSettings,
    ): EnrollmentEventDetails {
        val admitted =
            filter { it.programStage() in settings.admissionProgramStageIds }
                .mapNotNull { it.eventDate() }
                .minOrNull()
        val dischargeEvents = filter { it.programStage() in settings.dischargeProgramStageIds }
        val discharge = dischargeEvents.mapNotNull { it.eventDate() }.maxOrNull()

        return EnrollmentEventDetails(
            admitted = admitted,
            discharge = discharge,
            outcome = dischargeEvents.latestOutcome(settings.dischargeOutcomeDataElementIds),
        )
    }

    private fun List<Event>.latestOutcome(dischargeOutcomeDataElementIds: Set<String>): String? =
        mapNotNull { event ->
            event
                .trackedEntityDataValues()
                .orEmpty()
                .firstOrNull { dataValue ->
                    dataValue.dataElement() in dischargeOutcomeDataElementIds &&
                        !dataValue.value().isNullOrBlank()
                }?.let { dataValue -> event to dataValue }
        }.maxWithOrNull(
            compareBy(
                { (event) -> event.eventDate() ?: event.dueDate() ?: event.created() ?: Date(0) },
                { (event) -> event.uid().orEmpty() },
            ),
        )?.second
            ?.userFriendlyValue(d2)
            ?.takeIf { it.isNotBlank() }

    private data class EnrollmentEventDetails(
        val admitted: Date?,
        val discharge: Date?,
        val outcome: String?,
    )
}
