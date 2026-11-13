package com.opencloudgaming.opennow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutProfilesTest {
    private val custom = KeyboardOverlayButton("user_a", "letter_f", layoutIndex = 7)
    private val customLayout = AndroidTouchSettings(
        keyboardButtons = defaultKeyboardOverlayButtons() + custom,
        offsets = mapOf(
            "keyboard_button_user_a_landscape" to TouchOffset(0.2f, 0.3f),
            "A" to TouchOffset(0.5f, 0.5f),
        ),
        buttonAppearances = mapOf(
            "keyboard_custom_user_a" to TouchButtonAppearance(label = "Use"),
            "A" to TouchButtonAppearance(label = "Go"),
        ),
    )

    @Test
    fun `saving captures only the keyboard layout and makes the profile active`() {
        val saved = AppSettings(androidTouch = customLayout).savingKeyboardLayoutProfile(" Build \n")!!

        val profile = saved.keyboardLayoutProfiles.single()
        assertEquals("Build", profile.name)
        assertTrue(profile.buttons.any { it.id == "user_a" })
        assertEquals(TouchOffset(0.2f, 0.3f), profile.offsets["keyboard_button_user_a_landscape"])
        assertEquals("Use", profile.appearances["keyboard_custom_user_a"]?.label)
        assertTrue(profile.offsets.keys.all { it.startsWith("keyboard_") })
        assertTrue(profile.appearances.keys.all { it.startsWith("keyboard_") })
        assertEquals(profile.id, saved.activeKeyboardLayoutProfileId)
        assertFalse(saved.keyboardLayoutProfileModified())
    }

    @Test
    fun `applying restores the layout, replaces old keyboard keys and keeps gamepad keys`() {
        val saved = AppSettings(androidTouch = customLayout).savingKeyboardLayoutProfile("Build")!!
        val id = saved.activeKeyboardLayoutProfileId!!
        val other = saved.copy(
            androidTouch = AndroidTouchSettings(
                keyboardButtons = defaultKeyboardOverlayButtons(),
                offsets = mapOf(
                    "keyboard_button_${DEFAULT_KEYBOARD_F_ID}_landscape" to TouchOffset(0.9f, 0.9f),
                    "A" to TouchOffset(0.1f, 0.1f),
                ),
                buttonAppearances = mapOf("keyboard_lmb" to TouchButtonAppearance(label = "Old")),
            ),
        )

        val applied = other.applyingKeyboardLayoutProfile(id).androidTouch

        assertTrue(applied.keyboardButtons.any { it.id == "user_a" })
        assertEquals(TouchOffset(0.2f, 0.3f), applied.offsets["keyboard_button_user_a_landscape"])
        assertEquals(TouchOffset(0.1f, 0.1f), applied.offsets["A"])
        assertFalse(applied.offsets["keyboard_button_${DEFAULT_KEYBOARD_F_ID}_landscape"] == TouchOffset(0.9f, 0.9f))
        assertNull(applied.buttonAppearances["keyboard_lmb"])
        assertEquals("Use", applied.buttonAppearances["keyboard_custom_user_a"]?.label)
        assertEquals(id, other.applyingKeyboardLayoutProfile(id).activeKeyboardLayoutProfileId)
        assertEquals(other, other.applyingKeyboardLayoutProfile("missing"))
    }

    @Test
    fun `moving a button marks the active layout modified until it is updated`() {
        val saved = AppSettings(androidTouch = customLayout).savingKeyboardLayoutProfile("Build")!!
        val id = saved.activeKeyboardLayoutProfileId!!
        val moved = saved.copy(
            androidTouch = saved.androidTouch.copy(
                offsets = saved.androidTouch.offsets + ("keyboard_button_user_a_landscape" to TouchOffset(0.6f, 0.6f)),
            ),
        )

        assertTrue(moved.keyboardLayoutProfileModified())
        val updated = moved.updatingKeyboardLayoutProfile(id)
        assertFalse(updated.keyboardLayoutProfileModified())
        assertEquals(TouchOffset(0.6f, 0.6f), updated.keyboardLayoutProfiles.single().offsets["keyboard_button_user_a_landscape"])
    }

    @Test
    fun `gamepad edits do not mark the keyboard layout modified`() {
        val saved = AppSettings(androidTouch = customLayout).savingKeyboardLayoutProfile("Build")!!
        val edited = saved.copy(androidTouch = saved.androidTouch.copy(offsets = saved.androidTouch.offsets + ("A" to TouchOffset(0.9f, 0.9f))))
        assertFalse(edited.keyboardLayoutProfileModified())
    }

    @Test
    fun `rename delete and limit behave like other profiles`() {
        val saved = AppSettings().savingKeyboardLayoutProfile("Old")!!
        val id = saved.activeKeyboardLayoutProfileId!!
        assertEquals("New", saved.renamingKeyboardLayoutProfile(id, " New ").keyboardLayoutProfiles.single().name)
        assertEquals("Old", saved.renamingKeyboardLayoutProfile(id, "  ").keyboardLayoutProfiles.single().name)
        assertNull(saved.deletingKeyboardLayoutProfile(id).activeKeyboardLayoutProfileId)
        assertNull(AppSettings().savingKeyboardLayoutProfile("  "))

        var settings = AppSettings()
        repeat(MAX_KEYBOARD_LAYOUT_PROFILES) { settings = settings.savingKeyboardLayoutProfile("P$it")!! }
        assertFalse(settings.canSaveKeyboardLayoutProfile())
        assertNull(settings.savingKeyboardLayoutProfile("Extra"))
    }

    @Test
    fun `normalization drops foreign keys, invalid profiles and dangling active ids`() {
        val good = KeyboardLayoutProfile(
            id = "a",
            name = " Good ",
            buttons = defaultKeyboardOverlayButtons() + custom,
            offsets = mapOf(
                "A" to TouchOffset(1f, 1f),
                "keyboard_button_user_a_landscape" to TouchOffset(Float.NaN, 0.4f),
                "keyboard_button_ghost_landscape" to TouchOffset(0.1f, 0.1f),
            ),
        )
        val settings = AppSettings(
            keyboardLayoutProfiles = listOf(good, good.copy(name = "Dup"), good.copy(id = "", name = "No id"), good.copy(id = "b", name = " ")),
            activeKeyboardLayoutProfileId = "gone",
        ).normalizedForAndroid()

        val profile = settings.keyboardLayoutProfiles.single()
        assertEquals("Good", profile.name)
        assertNull(profile.offsets["A"])
        assertNull(profile.offsets["keyboard_button_ghost_landscape"])
        assertEquals(0f, profile.offsets["keyboard_button_user_a_landscape"]!!.x, 0f)
        assertNull(settings.activeKeyboardLayoutProfileId)
    }
}
