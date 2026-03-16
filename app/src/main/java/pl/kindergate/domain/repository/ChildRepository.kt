package pl.kindergate.domain.repository

import pl.kindergate.domain.model.ChildProfile

/**
 * Persistence contract for child profiles.
 *
 * MVP: only one profile per device is expected, but the interface is designed
 * to support multiple children without breaking changes.
 *
 * Implementations: [pl.kindergate.data.repository.ChildRepositoryImpl] (Room).
 */
interface ChildRepository {

    /** Returns all stored child profiles, ordered by creation time ascending. */
    suspend fun getChildren(): List<ChildProfile>

    /** Returns the profile with the given [id], or null if not found. */
    suspend fun getChildById(id: String): ChildProfile?

    /**
     * Inserts or updates the given [child] profile.
     * Identified by [ChildProfile.id]; if a profile with that id already exists
     * it is fully overwritten (upsert semantics).
     */
    suspend fun upsertChild(child: ChildProfile)

    /** Permanently removes the profile with [id]. No-op if not found. */
    suspend fun deleteChild(id: String)
}
