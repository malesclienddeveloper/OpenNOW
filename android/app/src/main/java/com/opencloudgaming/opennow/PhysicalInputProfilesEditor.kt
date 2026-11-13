package com.opencloudgaming.opennow

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opencloudgaming.opennow.ui.controls.ControlActionRow

private enum class ProfileNameDialog { None, Save, Rename }

/**
 * Settings UI for [PhysicalInputProfile]s: save the live tuning under a name, then use, update,
 * rename or delete it. All state changes go through the pure helpers in PhysicalInputProfiles.kt.
 */
@Composable
internal fun PhysicalInputProfilesEditor(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf(ProfileNameDialog.None) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    val profiles = settings.physicalInputProfiles
    val canSave = settings.canSavePhysicalInputProfile()
    val modified = settings.physicalInputProfileModified()
    val defaultName = stringResource(R.string.settings_physical_profile_default_name, profiles.size + 1)

    Text(
        stringResource(R.string.settings_physical_profiles_description),
        color = SettingsTextMuted,
        style = MaterialTheme.typography.bodySmall,
    )
    if (profiles.isEmpty()) {
        Text(
            stringResource(R.string.settings_physical_profiles_empty),
            color = SettingsTextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    profiles.forEach { profile ->
        val active = profile.id == settings.activePhysicalInputProfileId
        HorizontalDivider()
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleSmall)
            if (active) {
                Text(
                    stringResource(
                        if (modified) R.string.settings_physical_profile_modified
                        else R.string.settings_physical_profile_active,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { onSettingsChange(settings.applyingPhysicalInputProfile(profile.id)) }) {
                    Text(stringResource(R.string.settings_physical_profile_use))
                }
                TextButton(onClick = { onSettingsChange(settings.updatingPhysicalInputProfile(profile.id)) }) {
                    Text(stringResource(R.string.settings_physical_profile_update))
                }
                TextButton(onClick = {
                    renameId = profile.id
                    nameInput = profile.name
                    dialog = ProfileNameDialog.Rename
                }) { Text(stringResource(R.string.settings_physical_profile_rename)) }
                TextButton(onClick = { onSettingsChange(settings.deletingPhysicalInputProfile(profile.id)) }) {
                    Text(stringResource(R.string.settings_physical_profile_delete))
                }
            }
        }
    }
    if (profiles.isNotEmpty()) HorizontalDivider()
    ControlActionRow(
        label = stringResource(R.string.settings_physical_profile_save_current),
        actionLabel = stringResource(R.string.action_save),
        enabled = canSave,
        onClick = {
            renameId = null
            nameInput = defaultName
            dialog = ProfileNameDialog.Save
        },
    )
    if (!canSave) {
        Text(
            stringResource(R.string.settings_physical_profile_limit, MAX_PHYSICAL_INPUT_PROFILES),
            color = SettingsTextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (dialog != ProfileNameDialog.None) {
        val saving = dialog == ProfileNameDialog.Save
        val valid = sanitizePhysicalInputProfileName(nameInput).isNotBlank()
        val dismiss = { dialog = ProfileNameDialog.None; renameId = null }
        AlertDialog(
            onDismissRequest = dismiss,
            title = {
                Text(
                    stringResource(
                        if (saving) R.string.settings_physical_profile_save_current
                        else R.string.settings_physical_profile_rename,
                    ),
                )
            },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(MAX_PHYSICAL_INPUT_PROFILE_NAME) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_physical_profile_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        val id = renameId
                        val updated = if (saving) settings.savingPhysicalInputProfile(nameInput)
                        else id?.let { settings.renamingPhysicalInputProfile(it, nameInput) }
                        if (updated != null) onSettingsChange(updated)
                        dismiss()
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
