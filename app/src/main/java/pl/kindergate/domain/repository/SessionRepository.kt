package pl.kindergate.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.kindergate.domain.model.BlockSession
import pl.kindergate.domain.model.TamperEvent

interface SessionRepository {
    suspend fun insertBlockSession(session: BlockSession): Long
    suspend fun acknowledgeSession(sessionId: Long, elapsedMs: Long)
    fun observeTodaySessions(): Flow<List<BlockSession>>
    suspend fun getTodaySessions(): List<BlockSession>

    suspend fun insertTamperEvent(event: TamperEvent)
    fun observeTamperEvents(sinceMs: Long): Flow<List<TamperEvent>>
    suspend fun getUnacknowledgedTamperCount(): Int
}
