package com.rescuevpn.app.data.security

import javax.crypto.SecretKey

/** Supplies the non-exportable key used by the encrypted metadata codec. */
internal interface MetadataKeyProvider {
    /** Creates the key only when absent; must fail closed for an unusable existing key. */
    fun getOrCreateKey(): SecretKey
}
