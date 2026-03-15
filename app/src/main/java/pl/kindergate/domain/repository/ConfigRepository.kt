package pl.kindergate.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.kindergate.domain.model.AppConfig
import pl.kindergate.domain.model.PermissionStatus

interface ConfigRepository {
    fun observeConfig(): Flow<AppConfig>
    suspend fun getConfig(): AppConfig
    suspend fun updateConfig(config: AppConfig)
    suspend fun setOnboardingComplete()
    suspend fun setMonitoringEnabled(enabled: Boolean)
    suspend fun setBlockInterval(seconds: Int)

    // PIN management
    suspend fun setPinHash(pinHash: String)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun isPinConfigured(): Boolean

    // Permission status (read from system at call time, not persisted)
    suspend fun getPermissionStatus(): PermissionStatus
    fun observePermissionStatus(): Flow<PermissionStatus>
}
