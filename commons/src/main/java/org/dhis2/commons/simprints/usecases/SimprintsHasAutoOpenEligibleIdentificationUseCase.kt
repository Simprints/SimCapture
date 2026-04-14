package org.dhis2.commons.simprints.usecases

import android.os.Bundle

class SimprintsHasAutoOpenEligibleIdentificationUseCase {
    operator fun invoke(extras: Bundle?): Boolean =
        extras?.keySet()?.any { extraName ->
            extras.getString(extraName)?.let(::hasAutoOpenEligibleIdentification) == true
        } == true

    private fun hasAutoOpenEligibleIdentification(value: String): Boolean =
        identificationJsonObject.findAll(value).any { identification ->
            mfidCredentialLinkedKey.containsMatchIn(identification.value) &&
                !mfidCredentialVerifiedFalseKey.containsMatchIn(identification.value)
        }

    private companion object {
        private val identificationJsonObject = Regex("\\{[^{}]*\\}")
        private val mfidCredentialLinkedKey = Regex("\"isLinkedToCredential\"\\s*:\\s*true")
        private val mfidCredentialVerifiedFalseKey = Regex("\"isVerified\"\\s*:\\s*false")
    }
}
