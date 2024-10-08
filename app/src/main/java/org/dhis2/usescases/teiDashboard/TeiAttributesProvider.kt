package org.dhis2.usescases.teiDashboard

import com.simprints.simprints.Constants.SIMPRINTS_GUID
import io.reactivex.Single
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue

class TeiAttributesProvider(private val d2: D2) {

    fun getValuesFromTrackedEntityTypeAttributes(
        trackedEntityTypeUid: String?,
        trackedEntityInstanceUid: String,
    ): List<TrackedEntityAttributeValue> {
        val attrFromType = d2.trackedEntityModule().trackedEntityTypeAttributes()
            .byTrackedEntityTypeUid().eq(trackedEntityTypeUid)
            .byDisplayInList().isTrue.blockingGet().mapNotNull {
                it.trackedEntityAttribute()?.uid() to it
            }.toMap()

        return attrFromType.mapNotNull {
            getTrackedEntityAttributeValue(trackedEntityInstanceUid, it.key)
        }
    }

    fun getValuesFromProgramTrackedEntityAttributes(
        trackedEntityTypeUid: String?,
        trackedEntityInstanceUid: String,
    ): List<TrackedEntityAttributeValue> {
        val program = d2.programModule().programs()
            .byTrackedEntityTypeUid().eq(trackedEntityTypeUid).blockingGet()[0]
        val attrFromProgramTrackedEntityAttribute =
            d2.programModule().programTrackedEntityAttributes()
                .byProgram().eq(program.uid()).byDisplayInList().isTrue
                .blockingGet().mapNotNull {
                    it.trackedEntityAttribute()?.uid() to it
                }.toMap()

        return attrFromProgramTrackedEntityAttribute.mapNotNull {
            getTrackedEntityAttributeValue(trackedEntityInstanceUid, it.key)
        }
    }

    fun getValuesFromProgramTrackedEntityAttributesByProgram(
        programUid: String,
        trackedEntityInstanceUid: String,
    ): Single<List<TrackedEntityAttributeValue>> {
        val attrFromProgramTrackedEntityAttribute =
            d2.programModule().programTrackedEntityAttributes()
                .byProgram().eq(programUid).byDisplayInList().isTrue
                .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
                .blockingGet().mapNotNull {
                    it.trackedEntityAttribute()?.uid() to it
                }.toMap()

        return Single.just(
            attrFromProgramTrackedEntityAttribute.mapNotNull {
                getTrackedEntityAttributeValue(trackedEntityInstanceUid, it.key)
            },
        )
    }

    fun getListOfValuesFromProgramTrackedEntityAttributesByProgram(
        programUid: String,
        trackedEntityInstanceUid: String,
        excludedAttributeShortNames: List<String> = listOf(SIMPRINTS_GUID), // hide in TEI search
    ): List<TrackedEntityAttributeValue> {
        val attrFromProgramTrackedEntityAttribute =
            d2.programModule().programTrackedEntityAttributes()
                .byProgram().eq(programUid).byDisplayInList().isTrue
                .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
                .blockingGet().mapNotNull {
                    it.trackedEntityAttribute()?.uid() to it
                }.toMap()
                .filter { (attributeUid, _) ->
                    val attributeShortName =
                        transformAttributeToValueMap(trackedEntityInstanceUid, attributeUid).first
                            ?.shortName()
                    attributeShortName !in excludedAttributeShortNames
                }

        return attrFromProgramTrackedEntityAttribute.mapNotNull {
            getTrackedEntityAttributeValue(trackedEntityInstanceUid, it.key)
        }
    }

    fun getProgramTrackedEntityAttributesByProgram(
        programUid: String,
        trackedEntityInstanceUid: String,
    ): Single<List<Pair<TrackedEntityAttribute?, TrackedEntityAttributeValue?>>> {
        val attrFromProgramTrackedEntityAttribute =
            d2.programModule().programTrackedEntityAttributes()
                .byProgram().eq(programUid).byDisplayInList().isTrue
                .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
                .blockingGet()

        return Single.just(
            attrFromProgramTrackedEntityAttribute.map {
                transformAttributeToValueMap(trackedEntityInstanceUid, it.trackedEntityAttribute()?.uid())
            },
        )
    }

    private fun getTrackedEntityAttributeValue(
        trackedEntityInstanceUid: String,
        trackedEntityAttributeUid: String?,
    ): TrackedEntityAttributeValue? {
        return d2.trackedEntityModule().trackedEntityAttributeValues()
            .byTrackedEntityInstance().eq(trackedEntityInstanceUid)
            .byTrackedEntityAttribute().eq(trackedEntityAttributeUid)
            .one()
            .blockingGet()
    }

    private fun transformAttributeToValueMap(
        trackedEntityInstanceUid: String,
        trackedEntityAttributeUid: String?,
    ): Pair<TrackedEntityAttribute?, TrackedEntityAttributeValue?> {
        val teiAttribute = d2.trackedEntityModule()
            .trackedEntityAttributes().uid(trackedEntityAttributeUid).blockingGet()
        val teiAttributeValue = d2.trackedEntityModule().trackedEntityAttributeValues()
            .byTrackedEntityInstance().eq(trackedEntityInstanceUid)
            .byTrackedEntityAttribute().eq(trackedEntityAttributeUid)
            .one()
            .blockingGet()

        return Pair(teiAttribute, teiAttributeValue)
    }
}
