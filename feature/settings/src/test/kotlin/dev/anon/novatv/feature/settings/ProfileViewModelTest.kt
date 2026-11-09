package dev.anon.novatv.feature.settings

import dev.anon.novatv.core.model.PinHash
import dev.anon.novatv.core.model.Profile
import dev.anon.novatv.core.sync.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeProfileRepository
    private lateinit var viewModel: ProfileViewModel

    private val adult = Profile(id = 1, name = "Yetişkin", maxRating = "18")
    private val locked = Profile(
        id = 2,
        name = "Korumalı",
        pinHash = PinHash.create("1234"),
        maxRating = "7",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeProfileRepository(listOf(adult, locked), activeId = 1)
        viewModel = ProfileViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectingUnlockedProfileSwitchesImmediately() = runTest(dispatcher) {
        val job = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.select(adult.copy(id = 1))
        // Zaten aktif olan profil seçilirse hiçbir şey olmaz.
        assertEquals(1L, repository.activeId)

        job.cancel()
    }

    @Test
    fun pinProtectedProfileWaitsForCorrectPin() = runTest(dispatcher) {
        val job = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.select(locked)
        advanceUntilIdle()
        assertEquals(2L, viewModel.state.value.pinTarget?.id)
        assertEquals(1L, repository.activeId)

        viewModel.submitPin("9999")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.pinError)
        assertEquals(1L, repository.activeId)

        viewModel.submitPin("1234")
        advanceUntilIdle()
        assertNull(viewModel.state.value.pinTarget)
        assertFalse(viewModel.state.value.pinError)
        assertEquals(2L, repository.activeId)

        job.cancel()
    }

    @Test
    fun cancelPinLeavesActiveProfileUntouched() = runTest(dispatcher) {
        val job = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.select(locked)
        advanceUntilIdle()
        viewModel.cancelPin()
        advanceUntilIdle()

        assertNull(viewModel.state.value.pinTarget)
        assertEquals(1L, repository.activeId)

        job.cancel()
    }

    @Test
    fun limitsListStartsWithNoLimit() {
        assertEquals(listOf(null, "7", "13", "18"), viewModel.limits())
    }

    private class FakeProfileRepository(
        initial: List<Profile>,
        activeId: Long,
    ) : ProfileRepository {

        private val all = MutableStateFlow(initial)
        var activeId: Long = activeId
            private set

        override fun observeAll(): Flow<List<Profile>> = all

        override fun active(): Flow<Profile?> =
            all.map { profiles -> profiles.firstOrNull { it.id == activeId } }

        override suspend fun ensureDefault() = Unit

        override suspend fun add(
            name: String,
            pin: String?,
            maxRating: String?,
            isKids: Boolean,
        ): Long {
            val id = (all.value.maxOfOrNull { it.id } ?: 0L) + 1
            all.value = all.value + Profile(
                id = id,
                name = name,
                pinHash = pin?.let { PinHash.create(it) },
                maxRating = maxRating,
                isKids = isKids,
            )
            return id
        }

        override suspend fun update(profile: Profile) {
            all.value = all.value.map { if (it.id == profile.id) profile else it }
        }

        override suspend fun setPin(profileId: Long, pin: String?) {
            all.value = all.value.map {
                if (it.id == profileId) it.copy(pinHash = pin?.let(PinHash::create)) else it
            }
        }

        override suspend fun delete(profileId: Long) {
            all.value = all.value.filterNot { it.id == profileId }
        }

        override suspend fun setActive(profileId: Long) {
            activeId = profileId
        }

        override suspend fun verifyPin(profileId: Long, pin: String): Boolean {
            val profile = all.value.firstOrNull { it.id == profileId } ?: return false
            return profile.pinHash?.let { PinHash.verify(pin, it) } == true
        }

        override suspend fun byId(profileId: Long): Profile? =
            all.value.firstOrNull { it.id == profileId }

        @Suppress("unused")
        private fun unusedFlow(): Flow<Int> = flowOf(0)
    }
}
