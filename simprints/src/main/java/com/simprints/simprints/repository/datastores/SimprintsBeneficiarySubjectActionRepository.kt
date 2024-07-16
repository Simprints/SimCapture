package com.simprints.simprints.repository.datastores

import com.simprints.simprints.Constants.SIMPRINTS_SUBJECT_ACTIONS
import org.hisp.dhis.android.core.D2
import javax.inject.Inject

/**
 * Beneficiaries of Simprints biometrics can have biometric reference samples (templates),
 * data for which in the DHIS2 data model can be stored for each biometrically enrolled TEI
 * in a tracked entity attribute with short name simprintsSubjectActions, in JSON-encoded text.
 *
 * Reads/writes of Simprints subject actions are done via DHIS2 SDK, and are persisting to device storage.
 * Modification of that data for a TEI's Simprints will mark TEI as syncable, as usually in a DHIS2 app.
 * Syncing that TEI will write the new state of their Simprints subject actions to DHIS2 instance backend,
 * so other instances of this SimCapture app can then sync the new Simprints subject actions to themselves
 * and they then can use biometrics integration with Simprints ID for that TEI as well.
 */
class SimprintsBeneficiarySubjectActionRepository @Inject constructor(
    private val d2: D2,
) {

    fun getSimprintsSubjectActions(teiUid: String): String? =
        d2.trackedEntityModule().trackedEntityAttributes()
            .byShortName().eq(SIMPRINTS_SUBJECT_ACTIONS).blockingGet().mapNotNull {
                d2.trackedEntityModule().trackedEntityAttributeValues()
                    .value(it.uid(), teiUid).blockingGet()?.value()
            }.firstOrNull()

    fun setSimprintsSubjectActions(teiUid: String, subjectActions: String) {
        d2.trackedEntityModule().trackedEntityAttributes()
            .byShortName().eq(SIMPRINTS_SUBJECT_ACTIONS).blockingGet().forEach { attribute ->
                d2.trackedEntityModule().trackedEntityAttributeValues()
                    .value(attribute.uid(), teiUid)
                    .blockingSet(subjectActions)
            }
    }
}
