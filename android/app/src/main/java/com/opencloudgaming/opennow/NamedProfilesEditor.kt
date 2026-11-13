package com.opencloudgaming.opennow

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.opencloudgaming.opennow.ui.controls.ControlSection
import com.opencloudgaming.opennow.ui.controls.controlRowStyle

/** The part of a saved profile the shared UI needs; each profile type maps itself onto this. */
internal data class NamedProfile(val id: String, val name: String)

private enum class ProfileNameDialog { None, Save, Rename }

@Composable
private fun profileStatusLabel(modified: Boolean): String =
    stringResource(if (modified) R.string.input_profile_modified else R.string.input_profile_active)

/**
 * One manager UI for every named input profile: save the live setup under a name, then use, update,
 * rename or delete it. It owns only presentation and the name dialog; what a profile contains and
 * how it is applied stays with the caller, so physical input and keyboard layouts cannot drift apart.
 */
@Composable
internal fun NamedProfilesEditor(
    profiles: List<NamedProfile>,
    activeId: String?,
    modified: Boolean,
    canSave: Boolean,
    maxProfiles: Int,
    description: String,
    emptyText: String,
    saveLabel: String,
    onSave: (String) -> Unit,
    onUse: (String) -> Unit,
    onUpdate: (String) -> Unit,
    onRename: (id: String, name: String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf(ProfileNameDialog.None) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    val mutedColor = controlRowStyle().supportingColor
    val defaultName = stringResource(R.string.input_profile_default_name, profiles.size + 1)

    Text(description, color = mutedColor, style = MaterialTheme.typography.bodySmall)
    if (profiles.isEmpty()) Text(emptyText, color = mutedColor, style = MaterialTheme.typography.bodySmall)
    profiles.forEach { profile ->
        HorizontalDivider()
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleSmall)
            if (profile.id == activeId) {
                Text(
                    profileStatusLabel(modified),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { onUse(profile.id) }) { Text(stringResource(R.string.input_profile_use)) }
                TextButton(onClick = { onUpdate(profile.id) }) { Text(stringResource(R.string.input_profile_update)) }
                TextButton(onClick = {
                    renameId = profile.id
                    nameInput = profile.name
                    dialog = ProfileNameDialog.Rename
                }) { Text(stringResource(R.string.input_profile_rename)) }
                TextButton(onClick = { onDelete(profile.id) }) { Text(stringResource(R.string.input_profile_delete)) }
            }
        }
    }
    if (profiles.isNotEmpty()) HorizontalDivider()
    ControlActionRow(
        label = saveLabel,
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
            stringResource(R.string.input_profile_limit, maxProfiles),
            color = mutedColor,
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (dialog != ProfileNameDialog.None) {
        val saving = dialog == ProfileNameDialog.Save
        val dismiss = { dialog = ProfileNameDialog.None; renameId = null }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(if (saving) saveLabel else stringResource(R.string.input_profile_rename)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(MAX_PROFILE_NAME_LENGTH) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.input_profile_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = sanitizeProfileName(nameInput).isNotBlank(),
                    onClick = {
                        val id = renameId
                        if (saving) onSave(nameInput) else if (id != null) onRename(id, nameInput)
                        dismiss()
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** In-stream quick switch: one tap per saved profile, no editing, so it is safe to show mid-game. */
@Composable
internal fun NamedProfileQuickSwitch(
    title: String,
    profiles: List<NamedProfile>,
    activeId: String?,
    modified: Boolean,
    onSelect: (String) -> Unit,
) {
    ControlSection(title) { NamedProfileQuickSwitchRows(profiles, activeId, modified, onSelect) }
}

@Composable
private fun ColumnScope.NamedProfileQuickSwitchRows(
    profiles: List<NamedProfile>,
    activeId: String?,
    modified: Boolean,
    onSelect: (String) -> Unit,
) {
    profiles.forEach { profile ->
        ControlActionRow(
            label = profile.name,
            actionLabel = stringResource(R.string.input_profile_use),
            onClick = { onSelect(profile.id) },
            value = if (profile.id == activeId) profileStatusLabel(modified) else null,
        )
    }
}
