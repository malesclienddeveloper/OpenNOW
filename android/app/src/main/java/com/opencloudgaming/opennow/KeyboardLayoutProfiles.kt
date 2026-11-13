package com.opencloudgaming.opennow

import kotlinx.serialization.Serializable
import java.util.UUID

internal const val MAX_KEYBOARD_LAYOUT_PROFILES = 20

private const val KEYBOARD_KEY_PREFIX = "keyboard_"

/**
 * A named snapshot of the on-screen keyboard overlay layout: which buttons exist, where each one
 * sits (per orientation) and how it looks. Gamepad overlay keys are never part of it, so switching
 * a keyboard layout leaves the touch gamepad setup alone.
 */
@Serializable
data class KeyboardLayoutProfile(
    val id: String,
    val name: String,
    val buttons: List<KeyboardOverlayButton> = defaultKeyboardOverlayButtons(),
    val offsets: Map<String, TouchOffset> = emptyMap(),
    val appearances: Map<String, TouchButtonAppearance> = emptyMap(),
)

/** Reads the keyboard-owned part of the overlay settings; the same shape is used for saving and comparing. */
private fun AndroidTouchSettings.keyboardLayout(id: String, name: String): KeyboardLayoutProfile {
    val normalized = normalizedTouchControls()
    return KeyboardLayoutProfile(
        id = id,
        name = name,
        buttons = normalized.keyboardButtons,
        offsets = normalized.offsets.filterKeys { it.startsWith(KEYBOARD_KEY_PREFIX) },
        appearances = normalized.buttonAppearances.filterKeys { it.startsWith(KEYBOARD_KEY_PREFIX) },
    )
}

/** Replaces only the keyboard layout and re-normalizes, which also drops keys of buttons that no longer exist. */
private fun AndroidTouchSettings.applyingKeyboardLayout(layout: KeyboardLayoutProfile): AndroidTouchSettings =
    copy(
        keyboardButtons = layout.buttons,
        offsets = offsets.filterKeys { !it.startsWith(KEYBOARD_KEY_PREFIX) } + layout.offsets,
        buttonAppearances = buttonAppearances.filterKeys { !it.startsWith(KEYBOARD_KEY_PREFIX) } + layout.appearances,
    ).normalizedTouchControls()

internal fun KeyboardLayoutProfile.normalized(): KeyboardLayoutProfile =
    AndroidTouchSettings().applyingKeyboardLayout(this).keyboardLayout(id, sanitizeProfileName(name))

internal fun List<KeyboardLayoutProfile>.normalizedKeyboardLayoutProfiles(): List<KeyboardLayoutProfile> =
    take(MAX_KEYBOARD_LAYOUT_PROFILES)
        .distinctBy { it.id }
        .map { it.normalized() }
        .filter { it.id.isNotBlank() && it.name.isNotBlank() }

internal fun AppSettings.activeKeyboardLayoutProfile(): KeyboardLayoutProfile? =
    activeKeyboardLayoutProfileId?.let { id -> keyboardLayoutProfiles.firstOrNull { it.id == id } }

/** True when the active profile no longer matches the live overlay, e.g. after dragging or restyling a button. */
internal fun AppSettings.keyboardLayoutProfileModified(): Boolean {
    val active = activeKeyboardLayoutProfile() ?: return false
    return active.normalized().copy(id = "", name = "") != androidTouch.keyboardLayout("", "")
}

internal fun AppSettings.canSaveKeyboardLayoutProfile(): Boolean =
    keyboardLayoutProfiles.size < MAX_KEYBOARD_LAYOUT_PROFILES

/** Saves the live layout as a new profile and makes it active; null when the name is blank or the list is full. */
internal fun AppSettings.savingKeyboardLayoutProfile(name: String): AppSettings? {
    val cleaned = sanitizeProfileName(name)
    if (cleaned.isBlank() || !canSaveKeyboardLayoutProfile()) return null
    val profile = androidTouch.keyboardLayout(UUID.randomUUID().toString(), cleaned)
    return copy(keyboardLayoutProfiles = keyboardLayoutProfiles + profile, activeKeyboardLayoutProfileId = profile.id)
}

/** Loads a profile into the live overlay. Unknown ids leave the settings untouched. */
internal fun AppSettings.applyingKeyboardLayoutProfile(id: String): AppSettings {
    val profile = keyboardLayoutProfiles.firstOrNull { it.id == id } ?: return this
    return copy(
        androidTouch = androidTouch.applyingKeyboardLayout(profile),
        activeKeyboardLayoutProfileId = profile.id,
    )
}

internal fun AppSettings.renamingKeyboardLayoutProfile(id: String, name: String): AppSettings {
    val cleaned = sanitizeProfileName(name)
    if (cleaned.isBlank()) return this
    return copy(keyboardLayoutProfiles = keyboardLayoutProfiles.map { if (it.id == id) it.copy(name = cleaned) else it })
}

/** Overwrites a profile with the live layout. */
internal fun AppSettings.updatingKeyboardLayoutProfile(id: String): AppSettings =
    copy(
        keyboardLayoutProfiles = keyboardLayoutProfiles.map {
            if (it.id == id) androidTouch.keyboardLayout(it.id, it.name) else it
        },
    )

internal fun AppSettings.deletingKeyboardLayoutProfile(id: String): AppSettings =
    copy(
        keyboardLayoutProfiles = keyboardLayoutProfiles.filterNot { it.id == id },
        activeKeyboardLayoutProfileId = activeKeyboardLayoutProfileId?.takeUnless { it == id },
    )
