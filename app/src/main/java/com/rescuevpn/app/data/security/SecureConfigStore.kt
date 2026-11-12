package com.rescuevpn.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * ذخیره‌سازی رمزنگاری‌شده با Android KeyStore سخت‌افزاری.
 * مهم: به هیچ‌وجه در صورت خطا داده‌ها را به متن ساده یا حافظه ناامن منتقل نمی‌کند.
 */
class SecureConfigStore(context: Context) {

    private val securePrefs: SharedPreferences

    init {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            securePrefs = EncryptedSharedPreferences.create(
                "rescue_wireguard_secure_vault",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            throw SecurityException("خطای امنیتی بحرانی در دسترسی به Android KeyStore: امکان ذخیره امن کلید وجود ندارد.", e)
        }
    }

    fun saveConfig(id: String, name: String, rawContent: String) {
        securePrefs.edit()
            .putString("cfg_content_$id", rawContent)
            .putString("cfg_name_$id", name)
            .apply()
    }

    fun getConfig(id: String): Pair<String, String>? {
        val content = securePrefs.getString("cfg_content_$id", null) ?: return null
        val name = securePrefs.getString("cfg_name_$id", "کانفیگ") ?: "کانفیگ"
        return Pair(name, content)
    }

    fun deleteConfig(id: String) {
        securePrefs.edit()
            .remove("cfg_content_$id")
            .remove("cfg_name_$id")
            .apply()
    }

    fun getAllConfigIds(): List<String> {
        return securePrefs.all.keys
            .filter { it.startsWith("cfg_content_") }
            .map { it.removePrefix("cfg_content_") }
    }

    fun saveActiveConfigId(id: String?) {
        securePrefs.edit().putString("active_config_id", id).apply()
    }

    fun getActiveConfigId(): String? {
        return securePrefs.getString("active_config_id", null)
    }

    fun clearAll() {
        securePrefs.edit().clear().commit()
    }
}
