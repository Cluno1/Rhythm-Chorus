package chromahub.rhythm.app.features.catalog.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogServerSettingsScreen(
    state: CatalogUiState,
    onEnroll: (String, String) -> Unit,
    onIssueInvite: (String, String, String, String, String, Boolean) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var serverUrl by rememberSaveable(state.serverUrl) {
        mutableStateOf(state.serverUrl.ifBlank { "http://175.178.242.232:8010" })
    }
    var inviteCode by remember { mutableStateOf("") }
    var adminUsername by remember { mutableStateOf("admin") }
    var adminPassword by remember { mutableStateOf("") }
    var userId by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var replaceExistingDevice by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("作品库设备登记") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "输入一次性邀请码后，本机会在 Android Keystore 生成不可导出的设备私钥。之后的请求自动签名，不再要求长期 Bearer Token。",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("服务器地址") },
                singleLine = true,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                label = { Text("一次性邀请码") },
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
                if (state.loading) CircularProgressIndicator() else Text("登记这台设备")
            }
            if (state.configured) {
                Text("本设备已登记", color = MaterialTheme.colorScheme.primary)
                OutlinedButton(
                    onClick = onClear,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("移除本机登记信息")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("管理员发放邀请码", style = MaterialTheme.typography.titleMedium)
            Text(
                "管理员账号和密码只用于本次请求，不会保存到手机。HTTP 网络可看到这些内容，这是当前部署已接受的风险。",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = adminUsername,
                onValueChange = { adminUsername = it },
                label = { Text("管理员账号") },
                singleLine = true,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = adminPassword,
                onValueChange = { adminPassword = it },
                label = { Text("管理员密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("用户 ID（唯一）") },
                singleLine = true,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("用户显示名（可选）") },
                singleLine = true,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("替换已有设备")
                    Text(
                        "用于换机或重装；新设备登记成功时，旧设备立即失效。",
                        style = MaterialTheme.typography.bodySmall,
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
                Text("生成一次性邀请码")
            }
            state.issuedInvite?.let { issued ->
                SelectionContainer {
                    Text("邀请码：$issued", style = MaterialTheme.typography.bodyLarge)
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
    }
}
