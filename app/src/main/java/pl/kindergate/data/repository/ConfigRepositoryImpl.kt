package pl.kindergate.data.repository

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.kindergate.data.local.prefs.SecurePreferencesManager
import pl.kindergate.domain.model.AppConfig
import pl.kindergate.domain.model.PermissionStatus
import pl.kindergate.domain.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SecurePreferencesManager
) : ConfigRepository {

    override fun observeConfig(): Flow<AppConfig> = flow {
        emit(getConfig())
    }

    override suspend fun getConfig(): AppConfig = AppConfig(
        blockIntervalSeconds = prefs.getBlockIntervalSeconds(),
        isMonitoringEnabled = prefs.isMonitoringEnabled(),
        hasCompletedOnboarding = prefs.isOnboardingComplete(),
        pinConfigured = prefs.isPinConfigured(),
        selectedChildId = prefs.getSelectedChildId(),
    )

    override suspend fun updateConfig(config: AppConfig) {
        prefs.setMonitoringEnabled(config.isMonitoringEnabled)
        prefs.setBlockIntervalSeconds(config.blockIntervalSeconds)
    }

    override suspend fun setOnboardingComplete() = prefs.setOnboardingComplete()

    override suspend fun setMonitoringEnabled(enabled: Boolean) = prefs.setMonitoringEnabled(enabled)

    override suspend fun setBlockInterval(seconds: Int) = prefs.setBlockIntervalSeconds(seconds)

    override suspend fun setPinHash(pinHash: String) = prefs.setPinHash(pinHash)

    override suspend fun verifyPin(pin: String): Boolean = prefs.verifyPin(pin)

    override suspend fun isPinConfigured(): Boolean = prefs.isPinConfigured()

    override suspend fun getPermissionStatus(): PermissionStatus = PermissionStatus(
        isUsageStatsGranted = checkUsageStatsPermission(),
        isOverlayGranted = Settings.canDrawOverlays(context),
        isNotificationGranted = checkNotificationPermission(),
        isBatteryOptimizationExempt = checkBatteryOptimizationExempt(),
        isAccessibilityEnabled = checkAccessibilityEnabled()
    )

    override fun observePermissionStatus(): Flow<PermissionStatus> = flow {
        emit(getPermissionStatus())
    }

    override suspend fun setSelectedChildId(childId: String) = prefs.setSelectedChildId(childId)

    override suspend fun getSelectedChildId(): String? = prefs.getSelectedChildId()

    private fun checkUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun checkBatteryOptimizationExempt(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun checkAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(context.packageName)
    }
}
