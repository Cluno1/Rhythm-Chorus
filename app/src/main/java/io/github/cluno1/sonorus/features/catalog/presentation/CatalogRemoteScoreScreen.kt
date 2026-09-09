package io.github.cluno1.sonorus.features.catalog.presentation

import io.github.cluno1.sonorus.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.cluno1.sonorus.features.scores.presentation.RemoteScoreScreen
import io.github.cluno1.sonorus.features.catalog.domain.ScoreRevision
import io.github.cluno1.sonorus.features.catalog.domain.MusicXmlRuntimeSanitizer
import io.github.cluno1.sonorus.features.catalog.domain.CatalogLibraryScoreWork
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import io.github.cluno1.sonorus.shared.presentation.components.common.M3CircularLoader
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon
import io.github.cluno1.sonorus.shared.presentation.components.icons.RhythmIcons

@Composable
fun CatalogRemoteScoreScreen(
    revisionId: String,
    title: String,
    scoreLabel: String,
    expectedPartCount: Int,
    scoreWork: CatalogLibraryScoreWork? = null,
    initialScoreId: String? = null,
    viewModel: CatalogViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedScoreId by remember(scoreWork?.workId, initialScoreId) {
        mutableStateOf(initialScoreId ?: scoreWork?.defaultScoreId)
    }
    val selectedOption = scoreWork?.scoreOptions?.firstOrNull { it.scoreId == selectedScoreId }
        ?: scoreWork?.scoreOptions?.firstOrNull { it.scoreId == scoreWork.defaultScoreId }
    val activeRevisionId = selectedOption?.revisionId ?: revisionId
    val activePartCount = selectedOption?.partCount ?: expectedPartCount
    var retry by remember { mutableIntStateOf(0) }
    var history by remember(activeRevisionId) { mutableStateOf<List<ScoreRevision>>(emptyList()) }
    var selectedIndex by remember(activeRevisionId) { mutableIntStateOf(0) }
    var bytes by remember(activeRevisionId) { mutableStateOf<ByteArray?>(null) }
    var error by remember(activeRevisionId) { mutableStateOf<String?>(null) }

    LaunchedEffect(activeRevisionId, retry) {
        history = emptyList()
        selectedIndex = 0
        bytes = null
        error = null
        viewModel.scoreHistory(activeRevisionId).fold(
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
            title = scoreWork?.title ?: title,
            canonicalMusicXml = checkNotNull(bytes),
            onBackClick = onBack,
            scoreLabel = selectedOption?.scoreLabel ?: scoreLabel.takeIf(String::isNotBlank),
            revisionLabel = history.getOrNull(selectedIndex)?.let {
                stringResource(R.string.score_revision_label, it.revisionNo)
            },
            canOpenNewerRevision = selectedIndex > 0,
            canOpenOlderRevision = selectedIndex < history.lastIndex,
            onOpenNewerRevision = { selectedIndex-- },
            onOpenOlderRevision = { selectedIndex++ },
            scoreSettingsContent = {
                scoreWork?.takeIf { it.scoreOptions.size > 1 }?.let { work ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    RhythmIcons.ScoreFilled,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    stringResource(R.string.score_version),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                work.scoreOptions.forEach { option ->
                                    FilterChip(
                                        selected = option.scoreId == selectedOption?.scoreId,
                                        onClick = { selectedScoreId = option.scoreId },
                                        label = {
                                            Text(
                                                "${option.scoreLabel} · ${stringResource(R.string.score_revision_label, option.revisionNo)}"
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            },
            expectedPartCount = activePartCount,
            modifier = modifier,
        )
        error != null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        RhythmIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        checkNotNull(error),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    FilledTonalButton(onClick = { retry++ }) {
                        Text(stringResource(R.string.score_retry))
                    }
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.score_back))
                    }
                }
            }
        }
        else -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            M3CircularLoader()
        }
    }
}
