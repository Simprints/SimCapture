package org.dhis2.commons.simprints.repository

import org.dhis2.commons.prefs.PreferenceProvider

class SimprintsSessionRepository(
    private val preferenceProvider: PreferenceProvider,
) {
    fun save(sessionId: String) {
        preferenceProvider.setValue(LAST_IDENTIFICATION_SESSION_ID, sessionId)
        clearPendingEnrollment()
    }

    fun get(): String? = preferenceProvider.getString(LAST_IDENTIFICATION_SESSION_ID)

    fun hasPendingSession(): Boolean = !get().isNullOrBlank()

    fun markPendingEnrollment() {
        if (hasPendingSession()) {
            preferenceProvider.setValue(PENDING_ENROLL_LAST, true)
        }
    }

    fun pendingEnrollmentSessionId(): String? =
        get()
            ?.takeIf(String::isNotBlank)
            ?.takeIf { preferenceProvider.getBoolean(PENDING_ENROLL_LAST, false) }

    fun hasPendingEnrollment(): Boolean = pendingEnrollmentSessionId() != null

    fun clearPendingEnrollment() = preferenceProvider.removeValue(PENDING_ENROLL_LAST)

    fun clear() {
        preferenceProvider.removeValue(LAST_IDENTIFICATION_SESSION_ID)
        clearPendingEnrollment()
    }

    internal companion object {
        internal const val LAST_IDENTIFICATION_SESSION_ID = "SID_LAST_IDENTIFICATION_SESSION_ID"
        internal const val PENDING_ENROLL_LAST = "SID_PENDING_ENROLL_LAST"
    }
}
