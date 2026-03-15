package pl.kindergate.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.kindergate.data.local.db.dao.BlockSessionDao
import pl.kindergate.data.local.db.dao.TamperEventDao
import pl.kindergate.data.local.db.entity.BlockSessionEntity
import pl.kindergate.data.local.db.entity.TamperEventEntity
import pl.kindergate.domain.model.BlockSession
import pl.kindergate.domain.model.TamperEvent
import pl.kindergate.domain.repository.SessionRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val blockSessionDao: BlockSessionDao,
    private val tamperEventDao: TamperEventDao
) : SessionRepository {

    override suspend fun insertBlockSession(session: BlockSession): Long =
        blockSessionDao.insert(BlockSessionEntity.fromDomain(session))

    override suspend fun acknowledgeSession(sessionId: Long, elapsedMs: Long) =
        blockSessionDao.acknowledge(sessionId, elapsedMs)

    override fun observeTodaySessions(): Flow<List<BlockSession>> =
        blockSessionDao.observeToday(startOfTodayMs()).map { list ->
            list.map(BlockSessionEntity::toDomain)
        }

    override suspend fun getTodaySessions(): List<BlockSession> =
        blockSessionDao.getToday(startOfTodayMs()).map(BlockSessionEntity::toDomain)

    override suspend fun insertTamperEvent(event: TamperEvent) =
        tamperEventDao.insert(TamperEventEntity.fromDomain(event))

    override fun observeTamperEvents(sinceMs: Long): Flow<List<TamperEvent>> =
        tamperEventDao.observeSince(sinceMs).map { list ->
            list.map(TamperEventEntity::toDomain)
        }

    override suspend fun getUnacknowledgedTamperCount(): Int =
        tamperEventDao.getUnacknowledgedCount()

    private fun startOfTodayMs(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
