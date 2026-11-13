package com.opencloudgaming.opennow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalInputProfilesTest {
    private val tuned = PhysicalInputSettings(stickDeadZone = 0.25f, swapAB = true, invertStickY = true)

    @Test
    fun `saving captures the live settings under a trimmed name and makes it active`() {
        val saved = AppSettings(physicalInput = tuned).savingPhysicalInputProfile("  Shooter \n")!!

        val profile = saved.physicalInputProfiles.single()
        assertEquals("Shooter", profile.name)
        assertEquals(tuned, profile.settings)
        assertEquals(profile.id, saved.activePhysicalInputProfileId)
        assertFalse(saved.physicalInputProfileModified())
    }

    @Test
    fun `saving rejects blank names and a full list`() {
        assertNull(AppSettings().savingPhysicalInputProfile("   "))

        var settings = AppSettings()
        repeat(MAX_PHYSICAL_INPUT_PROFILES) { settings = settings.savingPhysicalInputProfile("P$it")!! }
        assertFalse(settings.canSavePhysicalInputProfile())
        assertNull(settings.savingPhysicalInputProfile("One too many"))
    }

    @Test
    fun `applying a profile loads its settings and unknown ids change nothing`() {
        val base = AppSettings(physicalInput = tuned).savingPhysicalInputProfile("Tuned")!!
        val reset = base.copy(physicalInput = PhysicalInputSettings(), activePhysicalInputProfileId = null)
        val id = base.physicalInputProfiles.single().id

        val applied = reset.applyingPhysicalInputProfile(id)
        assertEquals(tuned, applied.physicalInput)
        assertEquals(id, applied.activePhysicalInputProfileId)
        assertEquals(reset, reset.applyingPhysicalInputProfile("missing"))
    }

    @Test
    fun `manual tuning marks the active profile modified until it is updated`() {
        val saved = AppSettings(physicalInput = tuned).savingPhysicalInputProfile("Tuned")!!
        val id = saved.activePhysicalInputProfileId!!
        val edited = saved.copy(physicalInput = tuned.copy(stickSensitivity = 1.5f))

        assertTrue(edited.physicalInputProfileModified())
        val updated = edited.updatingPhysicalInputProfile(id)
        assertFalse(updated.physicalInputProfileModified())
        assertEquals(1.5f, updated.physicalInputProfiles.single().settings.stickSensitivity, 0f)
    }

    @Test
    fun `renaming sanitizes the name and ignores blank input`() {
        val saved = AppSettings().savingPhysicalInputProfile("Old")!!
        val id = saved.activePhysicalInputProfileId!!

        assertEquals("New name", saved.renamingPhysicalInputProfile(id, " New\u0000 name ").physicalInputProfiles.single().name)
        assertEquals("Old", saved.renamingPhysicalInputProfile(id, "   ").physicalInputProfiles.single().name)
        assertEquals(
            MAX_PROFILE_NAME_LENGTH,
            saved.renamingPhysicalInputProfile(id, "x".repeat(200)).physicalInputProfiles.single().name.length,
        )
    }

    @Test
    fun `deleting clears the active id only for the deleted profile`() {
        val first = AppSettings().savingPhysicalInputProfile("A")!!
        val both = first.savingPhysicalInputProfile("B")!!
        val (a, b) = both.physicalInputProfiles

        assertEquals(b.id, both.activePhysicalInputProfileId)
        assertEquals(b.id, both.deletingPhysicalInputProfile(a.id).activePhysicalInputProfileId)
        val withoutActive = both.deletingPhysicalInputProfile(b.id)
        assertNull(withoutActive.activePhysicalInputProfileId)
        assertNull(withoutActive.activePhysicalInputProfile())
        assertEquals(listOf(a.id), withoutActive.physicalInputProfiles.map { it.id })
    }

    @Test
    fun `normalization drops invalid profiles and dangling active ids`() {
        val good = PhysicalInputProfile("a", "  Good ", PhysicalInputSettings(stickDeadZone = 9f))
        val settings = AppSettings(
            physicalInputProfiles = listOf(
                good,
                good.copy(name = "Duplicate id"),
                PhysicalInputProfile("", "No id"),
                PhysicalInputProfile("b", " \n "),
            ),
            activePhysicalInputProfileId = "gone",
        ).normalizedForAndroid()

        val profile = settings.physicalInputProfiles.single()
        assertEquals("Good", profile.name)
        assertEquals(0.4f, profile.settings.stickDeadZone, 0f)
        assertNull(settings.activePhysicalInputProfileId)
        assertEquals(
            "a",
            AppSettings(physicalInputProfiles = listOf(good), activePhysicalInputProfileId = "a")
                .normalizedForAndroid().activePhysicalInputProfileId,
        )
        assertNotNull(settings.physicalInput)
    }
}
