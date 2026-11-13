package com.opencloudgaming.opennow

import kotlinx.serialization.Serializable
import java.util.UUID

internal const val MAX_PHYSICAL_INPUT_PROFILES = 20

/**
 * A named snapshot of [PhysicalInputSettings] (dead zones, sensitivity, inversion and swaps for the
 * hardware controller, keyboard and mouse). Applying a profile copies its settings into
 * [AppSettings.physicalInput], which the stream client already observes, so a profile never needs
 * its own input path.
 */
@Serializable
data class PhysicalInputProfile(
    val id: String,
    val name: String,
    val settings: PhysicalInputSettings = PhysicalInputSettings(),
)

internal fun List<PhysicalInputProfile>.normalizedPhysicalInputProfiles(): List<PhysicalInputProfile> =
    take(MAX_PHYSICAL_INPUT_PROFILES)
        .distinctBy { it.id }
        .map { it.copy(name = sanitizeProfileName(it.name), settings = it.settings.normalized()) }
        .filter { it.id.isNotBlank() && it.name.isNotBlank() }

internal fun AppSettings.activePhysicalInputProfile(): PhysicalInputProfile? =
    activePhysicalInputProfileId?.let { id -> physicalInputProfiles.firstOrNull { it.id == id } }

/** True when the active profile no longer matches the live settings, e.g. after a manual slider change. */
internal fun AppSettings.physicalInputProfileModified(): Boolean =
    activePhysicalInputProfile()?.let { it.settings.normalized() != physicalInput.normalized() } ?: false

internal fun AppSettings.canSavePhysicalInputProfile(): Boolean =
    physicalInputProfiles.size < MAX_PHYSICAL_INPUT_PROFILES

/** Saves the live settings as a new profile and makes it active; null when the name is blank or the list is full. */
internal fun AppSettings.savingPhysicalInputProfile(name: String): AppSettings? {
    val cleaned = sanitizeProfileName(name)
    if (cleaned.isBlank() || !canSavePhysicalInputProfile()) return null
    val profile = PhysicalInputProfile(UUID.randomUUID().toString(), cleaned, physicalInput.normalized())
    return copy(physicalInputProfiles = physicalInputProfiles + profile, activePhysicalInputProfileId = profile.id)
}

/** Loads a profile into the live settings. Unknown ids leave the settings untouched. */
internal fun AppSettings.applyingPhysicalInputProfile(id: String): AppSettings {
    val profile = physicalInputProfiles.firstOrNull { it.id == id } ?: return this
    return copy(physicalInput = profile.settings.normalized(), activePhysicalInputProfileId = profile.id)
}

internal fun AppSettings.renamingPhysicalInputProfile(id: String, name: String): AppSettings {
    val cleaned = sanitizeProfileName(name)
    if (cleaned.isBlank()) return this
    return copy(physicalInputProfiles = physicalInputProfiles.map { if (it.id == id) it.copy(name = cleaned) else it })
}

/** Overwrites a profile with the live settings. */
internal fun AppSettings.updatingPhysicalInputProfile(id: String): AppSettings =
    copy(
        physicalInputProfiles = physicalInputProfiles.map {
            if (it.id == id) it.copy(settings = physicalInput.normalized()) else it
        },
    )

internal fun AppSettings.deletingPhysicalInputProfile(id: String): AppSettings =
    copy(
        physicalInputProfiles = physicalInputProfiles.filterNot { it.id == id },
        activePhysicalInputProfileId = activePhysicalInputProfileId?.takeUnless { it == id },
    )
