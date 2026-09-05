package io.github.cluno1.sonorus.features.catalog.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.cluno1.sonorus.R
import io.github.cluno1.sonorus.shared.presentation.components.common.CollapsibleHeaderScreen
import io.github.cluno1.sonorus.ui.LocalMiniPlayerPadding

@Composable
fun CatalogServerSettingsScreen(
    state: CatalogUiState,
    onEnroll: (String, String) -> Unit,
    onIssueInvite: (String, String, String, String, String, Boolean) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var serverUrl by rememberSaveable(state.serverUrl, state.deviceRegistered) {
        mutableStateOf(
            if (state.deviceRegistered) {
                state.serverUrl.ifBlank { PUBLIC_CATALOG_URL }
            } else {
                PUBLIC_CATALOG_URL
            },
        )
    }
    var inviteCode by remember { mutableStateOf("") }
    var adminUsername by remember { mutableStateOf("admin") }
    var adminPassword by remember { mutableStateOf("") }
    var userId by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var replaceExistingDevice by rememberSaveable { mutableStateOf(false) }

    val miniPlayerBottomPadding = LocalMiniPlayerPadding.current.calculateBottomPadding()

    CollapsibleHeaderScreen(
        title = stringResource(R.string.settings_catalog_server),
        showBackButton = true,
        onBackClick = onBack,
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .then(modifier)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp + miniPlayerBottomPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CatalogSettingsSection {
                Text(
                    stringResource(R.string.catalog_enrollment_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(stringResource(R.string.catalog_server_address)) },
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text(stringResource(R.string.catalog_one_time_invite)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onEnroll(serverUrl, inviteCode) },
                    enabled = !state.loading && serverUrl.isNotBlank() && inviteCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(R.string.catalog_enroll_this_device))
                    }
                }
                if (state.deviceRegistered) {
                    Text(
                        stringResource(R.string.catalog_device_registered),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(
                        onClick = onClear,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.catalog_remove_device_registration))
                    }
                } else if (state.configured) {
                    Text(
                        stringResource(R.string.catalog_legacy_connection_notice),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = onClear,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.catalog_remove_legacy_connection))
                    }
                }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            CatalogSettingsSection(title = stringResource(R.string.catalog_admin_issue_invite)) {
                Text(
                    stringResource(R.string.catalog_admin_http_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = adminUsername,
                    onValueChange = { adminUsername = it },
                    label = { Text(stringResource(R.string.catalog_admin_username)) },
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    label = { Text(stringResource(R.string.catalog_admin_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text(stringResource(R.string.catalog_user_id)) },
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.catalog_user_display_name)) },
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.catalog_replace_existing_device))
                        Text(
                            stringResource(R.string.catalog_replace_existing_device_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = replaceExistingDevice,
                        onCheckedChange = { replaceExistingDevice = it },
                        enabled = !state.loading,
                    )
                }
                OutlinedButton(
                    onClick = {
                        onIssueInvite(
                            serverUrl,
                            adminUsername,
                            adminPassword,
                            userId,
                            displayName,
                            replaceExistingDevice,
                        )
                        adminPassword = ""
                    },
                    enabled = !state.loading &&
                        serverUrl.isNotBlank() &&
                        adminUsername.isNotBlank() &&
                        adminPassword.isNotBlank() &&
                        userId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.catalog_generate_invite))
                }
                state.issuedInvite?.let { issued ->
                    SelectionContainer {
                        Text(
                            stringResource(R.string.catalog_invite_code_value, issued),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

private const val PUBLIC_CATALOG_URL = "http://175.178.242.232:8010"

@Composable
private fun CatalogSettingsSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}
