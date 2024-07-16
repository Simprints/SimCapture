package com.simprints.simprints.repository.datastores

import com.simprints.simprints.Constants.SIMPRINTS_GUID
import org.hisp.dhis.android.core.D2
import javax.inject.Inject

/**
 * Beneficiaries of Simprints biometrics have GUIDs, which in the DHIS2 data model are stored
 * for each biometrically enrolled TEI in a tracked entity attribute with short name simprintsGuid.
 *
 * Reads and writes of Simprints GUIDs are done via DHIS2 SDK, and are persisting to device storage.
 * Modification of a TEI's Simprints GUID will mark that TEI as syncable, as usually in a DHIS2 app.
 * Syncing that TEI will write the new state of their Simprints GUID to the DHIS2 instance backend,
 * so other instances of this SimCapture app can then sync the new Simprints GUID to themselves
 * and they then can use biometrics integration with Simprints ID for that TEI as well.
 */
class SimprintsBeneficiaryGuidRepository @Inject constructor(
    private val d2: D2,
) {

    fun getSimprintsGuid(teiUid: String): String? =
        d2.trackedEntityModule().trackedEntityAttributes()
            .byShortName().eq(SIMPRINTS_GUID).blockingGet().mapNotNull {
                d2.trackedEntityModule().trackedEntityAttributeValues()
                    .value(it.uid(), teiUid).blockingGet()?.value()
            }.firstOrNull()

    fun setSimprintsGuid(teiUid: String, simprintsGuid: String) {
        d2.trackedEntityModule().trackedEntityAttributes()
            .byShortName().eq(SIMPRINTS_GUID).blockingGet().forEach { attribute ->
                d2.trackedEntityModule().trackedEntityAttributeValues()
                    .value(attribute.uid(), teiUid)
                    .blockingSet(simprintsGuid)
            }
    }

    fun getTeiUidsInProgram(simprintsGuids: List<String>, programUid: String?): List<String> =
        getExistingGuidsPairedToTeiUids(simprintsGuids).filter { (_, teiUid) ->
            programUid == null ||
                !d2.trackedEntityModule().trackedEntityInstances()
                    .byUid().eq(teiUid)
                    .byProgramUids(listOfNotNull(programUid))
                    .blockingIsEmpty()
        }.map { (_, teiUid) ->
            teiUid
        }

    fun getExistingGuidsPairedToTeiUids(
        simprintsGuids: List<String>? = null,
    ): List<Pair<String, String>> {
        val simprintsGuidAttributes =
            d2.trackedEntityModule().trackedEntityAttributes()
                .byShortName().eq(SIMPRINTS_GUID).blockingGet()
        val allSimprintsGuidAttributeValues =
            simprintsGuidAttributes.flatMap { simprintsGuidAttribute ->
                d2.trackedEntityModule().trackedEntityAttributeValues()
                    .byTrackedEntityAttribute().eq(simprintsGuidAttribute.uid()).blockingGet()
            }
        val relevantSimprintsGuidAttributeValues =
            if (simprintsGuids != null) {
                allSimprintsGuidAttributeValues.filter { simprintsGuidAttributeValue ->
                    simprintsGuidAttributeValue.value() in simprintsGuids
                }
            } else {
                allSimprintsGuidAttributeValues
            }
        return relevantSimprintsGuidAttributeValues.mapNotNull { simprintsGuidAttributeValue ->
            val simprintsGuid = simprintsGuidAttributeValue.value()
            val teiUid = simprintsGuidAttributeValue.trackedEntityInstance()
            if (simprintsGuid != null && teiUid != null) {
                simprintsGuid to teiUid
            } else {
                null
            }
        }
    }

    fun getSimprintsGuidAttributeUids(): List<String> =
        d2.trackedEntityModule().trackedEntityAttributes().byShortName().eq(SIMPRINTS_GUID)
            .blockingGet().map { it.uid() }
}
