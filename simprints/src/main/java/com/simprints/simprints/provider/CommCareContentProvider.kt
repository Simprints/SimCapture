package com.simprints.simprints.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.simprints.simprints.repository.SimprintsBiometricsRepository

/**
 * CommCare-compatible co-sync biometric data content provider for Simprints ID.
 */
class CommCareContentProvider : ContentProvider() {

    companion object {
        private const val CASE_METADATA = 1
        private const val CASE_DATA = 2
        private const val AUTHORITY = "org.commcare.dalvik.case"
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "casedb/case", CASE_METADATA)
            addURI(AUTHORITY, "casedb/data/*", CASE_DATA)
        }

        private const val COLUMN_CASE_ID = "case_id"
        private const val COLUMN_DATUM_ID = "datum_id"
        private const val COLUMN_VALUE = "value"
        private const val SIMPRINTS_COSYNC_SUBJECT_ACTIONS = "subjectActions"

        lateinit var simprintsBiometricsRepository: SimprintsBiometricsRepository // singleton
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            CASE_METADATA -> queryCaseMetadata(projection)
            CASE_DATA -> queryCaseData(uri, projection)
            else -> null
        }
    }

    private fun queryCaseMetadata(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(arrayOf(COLUMN_CASE_ID))
        simprintsBiometricsRepository.getTeiUidsWithSimprintsGuids().forEach { teiUid ->
            cursor.addRow(arrayOf(teiUid))
        }
        return cursor
    }

    private fun queryCaseData(uri: Uri, projection: Array<out String>?): Cursor {
        val caseId = uri.lastPathSegment
        val cursor = MatrixCursor(arrayOf(COLUMN_DATUM_ID, COLUMN_VALUE))
        caseId?.let {
            cursor.addRow(
                arrayOf(
                    SIMPRINTS_COSYNC_SUBJECT_ACTIONS,
                    simprintsBiometricsRepository.getSimprintsSubjectActions(caseId),
                ),
            )
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Insert operation is not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Delete operation is not supported")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        throw UnsupportedOperationException("Update operation is not supported")
    }
}
