package com.opencloudgaming.opennow

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Settings UI for [PhysicalInputProfile]s; all state changes go through PhysicalInputProfiles.kt. */
@Composable
internal fun PhysicalInputProfilesEditor(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    NamedProfilesEditor(
        profiles = settings.physicalInputProfiles.map { NamedProfile(it.id, it.name) },
        activeId = settings.activePhysicalInputProfileId,
        modified = settings.physicalInputProfileModified(),
        canSave = settings.canSavePhysicalInputProfile(),
        maxProfiles = MAX_PHYSICAL_INPUT_PROFILES,
        description = stringResource(R.string.settings_physical_profiles_description),
        emptyText = stringResource(R.string.settings_physical_profiles_empty),
        saveLabel = stringResource(R.string.settings_physical_profile_save_current),
        onSave = { name -> settings.savingPhysicalInputProfile(name)?.let(onSettingsChange) },
        onUse = { onSettingsChange(settings.applyingPhysicalInputProfile(it)) },
        onUpdate = { onSettingsChange(settings.updatingPhysicalInputProfile(it)) },
        onRename = { id, name -> onSettingsChange(settings.renamingPhysicalInputProfile(id, name)) },
        onDelete = { onSettingsChange(settings.deletingPhysicalInputProfile(it)) },
    )
}
