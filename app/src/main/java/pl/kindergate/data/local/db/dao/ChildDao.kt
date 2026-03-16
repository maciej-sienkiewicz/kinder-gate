package pl.kindergate.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.kindergate.data.local.db.entity.ChildEntity

@Dao
interface ChildDao {

    @Query("SELECT * FROM children ORDER BY created_at_ms ASC")
    suspend fun getAll(): List<ChildEntity>

    @Query("SELECT * FROM children WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChildEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(child: ChildEntity)

    @Query("DELETE FROM children WHERE id = :id")
    suspend fun delete(id: String)
}
