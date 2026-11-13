package com.opencloudgaming.opennow

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Settings UI for [KeyboardLayoutProfile]s; all state changes go through KeyboardLayoutProfiles.kt. */
@Composable
internal fun KeyboardLayoutProfilesEditor(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    NamedProfilesEditor(
        profiles = settings.keyboardLayoutProfiles.map { NamedProfile(it.id, it.name) },
        activeId = settings.activeKeyboardLayoutProfileId,
        modified = settings.keyboardLayoutProfileModified(),
        canSave = settings.canSaveKeyboardLayoutProfile(),
        maxProfiles = MAX_KEYBOARD_LAYOUT_PROFILES,
        description = stringResource(R.string.settings_keyboard_layouts_description),
        emptyText = stringResource(R.string.settings_keyboard_layouts_empty),
        saveLabel = stringResource(R.string.settings_keyboard_layout_save_current),
        onSave = { name -> settings.savingKeyboardLayoutProfile(name)?.let(onSettingsChange) },
        onUse = { onSettingsChange(settings.applyingKeyboardLayoutProfile(it)) },
        onUpdate = { onSettingsChange(settings.updatingKeyboardLayoutProfile(it)) },
        onRename = { id, name -> onSettingsChange(settings.renamingKeyboardLayoutProfile(id, name)) },
        onDelete = { onSettingsChange(settings.deletingKeyboardLayoutProfile(it)) },
    )
}
