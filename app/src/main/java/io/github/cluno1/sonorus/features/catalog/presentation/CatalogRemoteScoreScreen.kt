package io.github.cluno1.sonorus.features.catalog.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.cluno1.sonorus.features.scores.presentation.RemoteScoreScreen
import io.github.cluno1.sonorus.features.catalog.domain.ScoreRevision
import io.github.cluno1.sonorus.features.catalog.domain.MusicXmlRuntimeSanitizer

@Composable
fun CatalogRemoteScoreScreen(
    revisionId: String,
    title: String,
    expectedPartCount: Int,
    viewModel: CatalogViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var retry by remember { mutableIntStateOf(0) }
    var history by remember(revisionId) { mutableStateOf<List<ScoreRevision>>(emptyList()) }
    var selectedIndex by remember(revisionId) { mutableIntStateOf(0) }
    var bytes by remember(revisionId) { mutableStateOf<ByteArray?>(null) }
    var error by remember(revisionId) { mutableStateOf<String?>(null) }

    LaunchedEffect(revisionId, retry) {
        history = emptyList()
        selectedIndex = 0
        bytes = null
        error = null
        viewModel.scoreHistory(revisionId).fold(
            onSuccess = { history = it },
            onFailure = { error = it.message ?: "谱面修订链加载失败" },
        )
    }

    LaunchedEffect(history, selectedIndex) {
        val revision = history.getOrNull(selectedIndex) ?: return@LaunchedEffect
        bytes = null
        error = null
        viewModel.scoreBytes(revision).fold(
            onSuccess = {
                runCatching { MusicXmlRuntimeSanitizer.forAlphaTab(it) }.fold(
                    onSuccess = { runtimeBytes -> bytes = runtimeBytes },
                    onFailure = { failure -> error = failure.message ?: "谱面安全检查失败" },
                )
            },
            onFailure = { error = it.message ?: "谱面文件下载失败" },
        )
    }

    when {
        bytes != null -> RemoteScoreScreen(
            title = title,
            canonicalMusicXml = checkNotNull(bytes),
            onBackClick = onBack,
            revisionLabel = history.getOrNull(selectedIndex)?.let { "修订 ${it.revisionNo}" },
            canOpenNewerRevision = selectedIndex > 0,
            canOpenOlderRevision = selectedIndex < history.lastIndex,
            onOpenNewerRevision = { selectedIndex-- },
            onOpenOlderRevision = { selectedIndex++ },
            expectedPartCount = expectedPartCount,
            modifier = modifier,
        )
        error != null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(checkNotNull(error), color = MaterialTheme.colorScheme.error)
                Button(onClick = { retry++ }, modifier = Modifier.padding(top = 12.dp)) { Text("重试") }
                Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("返回") }
            }
        }
        else -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}
