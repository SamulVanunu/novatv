package dev.anon.novatv.core.sync

import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.entity.ProfileEntity
import dev.anon.novatv.core.model.PinHash
import dev.anon.novatv.core.model.Profile
import dev.anon.novatv.core.settings.PreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface ProfileRepository {
    fun observeAll(): Flow<List<Profile>>
    fun active(): Flow<Profile?>
    suspend fun ensureDefault()
    suspend fun add(name: String, pin: String?, maxRating: String?, isKids: Boolean): Long
    suspend fun update(profile: Profile)
    suspend fun setPin(profileId: Long, pin: String?)
    suspend fun delete(profileId: Long)
    suspend fun setActive(profileId: Long)
    suspend fun verifyPin(profileId: Long, pin: String): Boolean
    suspend fun byId(profileId: Long): Profile?
}

class RoomProfileRepository(
    private val db: NovaDatabase,
    private val preferences: PreferenceStore,
) : ProfileRepository {

    override fun observeAll(): Flow<List<Profile>> =
        db.profileDao().observeAll().map { rows -> rows.map { it.toModel() } }

    override fun active(): Flow<Profile?> =
        combine(preferences.activeProfileId, observeAll()) { id, profiles ->
            profiles.firstOrNull { it.id == id } ?: profiles.firstOrNull()
        }

    override suspend fun ensureDefault() {
        if (db.profileDao().count() == 0) {
            db.profileDao().insert(
                ProfileEntity(
                    id = PreferenceStore.DEFAULT_PROFILE_ID,
                    name = "Ana Profil",
                    pinHash = null,
                    maxRating = null,
                    isKids = false,
                ),
            )
        }
        val activeId = preferences.activeProfileId.first()
        if (db.profileDao().byId(activeId) == null) {
            val fallback = db.profileDao().observeAll().first().firstOrNull()?.id
                ?: PreferenceStore.DEFAULT_PROFILE_ID
            preferences.setActiveProfile(fallback)
        }
    }

    override suspend fun add(name: String, pin: String?, maxRating: String?, isKids: Boolean): Long =
        db.profileDao().insert(
            ProfileEntity(
                name = name.trim().ifBlank { "Profil" },
                pinHash = pin?.takeIf { it.isNotBlank() }?.let(PinHash::create),
                maxRating = maxRating,
                isKids = isKids,
            ),
        )

    override suspend fun update(profile: Profile) {
        db.profileDao().update(
            ProfileEntity(
                id = profile.id,
                name = profile.name.trim().ifBlank { "Profil" },
                pinHash = profile.pinHash,
                maxRating = profile.maxRating,
                isKids = profile.isKids,
            ),
        )
    }

    override suspend fun setPin(profileId: Long, pin: String?) {
        val current = db.profileDao().byId(profileId) ?: return
        db.profileDao().update(
            current.copy(pinHash = pin?.takeIf { it.isNotBlank() }?.let(PinHash::create)),
        )
    }

    override suspend fun delete(profileId: Long) {
        if (db.profileDao().count() <= 1) return
        db.profileDao().deleteWithUserData(profileId, db.userDataDao())
        if (preferences.activeProfileId.first() == profileId) {
            val next = db.profileDao().observeAll().first().firstOrNull()
            if (next != null) preferences.setActiveProfile(next.id)
        }
    }

    override suspend fun setActive(profileId: Long) {
        if (db.profileDao().byId(profileId) != null) {
            preferences.setActiveProfile(profileId)
        }
    }

    override suspend fun verifyPin(profileId: Long, pin: String): Boolean {
        val stored = db.profileDao().byId(profileId)?.pinHash ?: return true
        return PinHash.verify(pin, stored)
    }

    override suspend fun byId(profileId: Long): Profile? =
        db.profileDao().byId(profileId)?.toModel()
}

private fun ProfileEntity.toModel(): Profile = Profile(
    id = id,
    name = name,
    pinHash = pinHash,
    maxRating = maxRating,
    isKids = isKids,
)
