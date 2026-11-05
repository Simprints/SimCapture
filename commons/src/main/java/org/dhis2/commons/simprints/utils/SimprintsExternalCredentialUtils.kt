package org.dhis2.commons.simprints.utils

import android.os.Bundle
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.Strictness

object SimprintsExternalCredentialUtils {
    private const val REGISTER_ACTION = "com.simprints.id.REGISTER"
    private const val REGISTER_LAST_ACTION = "com.simprints.id.REGISTER_LAST_BIOMETRICS"
    private const val SCANNED_CREDENTIAL_EXTRA = "scannedCredential"
    private val gson = GsonBuilder().setStrictness(Strictness.STRICT).create()

    fun externalCredentialValue(extras: Bundle?): String? {
        val credentialJson = extras?.getString(SCANNED_CREDENTIAL_EXTRA) ?: return null
        val credential =
            try {
                gson.fromJson(credentialJson, JsonObject::class.java)
            } catch (_: JsonParseException) {
                null
            } ?: return null

        if (credential.stringValue("type") != "QRCode") return null
        return credential.stringValue("value")?.takeIf(String::isNotBlank)
    }

    fun enrollmentExternalCredentialValue(
        action: String?,
        guid: String?,
        extras: Bundle?,
    ): String? {
        if (action != REGISTER_ACTION && action != REGISTER_LAST_ACTION) return null
        if (guid.isNullOrBlank()) return null
        if (extras?.containsKey("identification") == true || extras?.containsKey("identifications") == true) return null
        return externalCredentialValue(extras)
    }

    private fun JsonObject.stringValue(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
}
