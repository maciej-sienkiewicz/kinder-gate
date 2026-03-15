package pl.kindergate.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.kindergate.data.repository.ConfigRepositoryImpl
import pl.kindergate.data.repository.MonitoredAppsRepositoryImpl
import pl.kindergate.data.repository.SessionRepositoryImpl
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.repository.MonitoredAppsRepository
import pl.kindergate.domain.repository.SessionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMonitoredAppsRepository(
        impl: MonitoredAppsRepositoryImpl
    ): MonitoredAppsRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(
        impl: ConfigRepositoryImpl
    ): ConfigRepository
}
