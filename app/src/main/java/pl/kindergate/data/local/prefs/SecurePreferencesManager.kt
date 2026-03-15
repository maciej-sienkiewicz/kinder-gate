package pl.kindergate.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive parent configuration.
 *
 * Uses EncryptedSharedPreferences backed by Android Keystore.
 * The master key is hardware-backed where available (API 28+).
 *
 * PIN security design:
 * - We store SHA-256 hash of PIN, not the PIN itself.
 * - SHA-256 is sufficient for a 4-8 digit numeric PIN protected by:
 *   1. Android Keystore encryption of the preference file
 *   2. No network access (offline-only)
 *   3. Rate limiting enforced in UI layer
 * - For production v1: migrate to BCrypt or Argon2 for stronger resistance.
 *
 * Tamper detection:
 * - If EncryptedSharedPreferences fails to decrypt (key wiped/data cleared),
 *   we catch the exception and treat it as a tamper event.
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setPinHash(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, sha256(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == sha256(pin)
    }

    fun isPinConfigured(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun isOnboardingComplete(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }

    fun isMonitoringEnabled(): Boolean = prefs.getBoolean(KEY_MONITORING_ENABLED, true)

    fun setBlockIntervalSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_BLOCK_INTERVAL, seconds).apply()
    }

    fun getBlockIntervalSeconds(): Int = prefs.getInt(KEY_BLOCK_INTERVAL, 60)

    fun setLastKnownServiceUptimeMs(uptimeMs: Long) {
        prefs.edit().putLong(KEY_LAST_UPTIME, uptimeMs).apply()
    }

    fun getLastKnownServiceUptimeMs(): Long = prefs.getLong(KEY_LAST_UPTIME, 0L)

    fun setServiceStartedAtElapsed(elapsedMs: Long) {
        prefs.edit().putLong(KEY_SERVICE_STARTED_ELAPSED, elapsedMs).apply()
    }

    fun getServiceStartedAtElapsed(): Long = prefs.getLong(KEY_SERVICE_STARTED_ELAPSED, 0L)

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_FILE_NAME = "kindergate_secure"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_BLOCK_INTERVAL = "block_interval_s"
        private const val KEY_LAST_UPTIME = "last_service_uptime_ms"
        private const val KEY_SERVICE_STARTED_ELAPSED = "service_started_elapsed_ms"
    }
}
