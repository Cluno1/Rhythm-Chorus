package chromahub.rhythm.app.features.streaming.presentation.screens

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.streaming.presentation.model.StreamingServiceOption
import chromahub.rhythm.app.features.streaming.presentation.model.StreamingServiceOptions
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledIconButton
import chromahub.rhythm.app.features.streaming.domain.model.StreamingServiceId
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType

@Composable
fun StreamingHomeScreen(
    viewModel: StreamingMusicViewModel,
    onNavigateToSettings: () -> Unit,
    onConfigureService: (String) -> Unit,
    onSwitchToLocalMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appSettings = remember { AppSettings.getInstance(context) }

    val selectedService by appSettings.streamingService.collectAsState()
    val appMode by appSettings.appMode.collectAsState()
    val serviceSessions by viewModel.serviceSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val displaySelectedService = remember(selectedService) {
        if (selectedService == StreamingServiceOptions.SUBSONIC) "" else selectedService
    }

    CollapsibleHeaderScreen(
        title = "",
        headerDisplayMode = 1,
        actions = {
            ExpressiveFilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                    onNavigateToSettings()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.Settings,
                    contentDescription = stringResource(id = R.string.home_settings_cd),
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    ) { contentModifier ->
        LazyColumn(
            modifier = modifier
                .then(contentModifier)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.rhythm_splash_logo),
                            contentDescription = stringResource(R.string.updates_rhythm_logo_cd),
                            modifier = Modifier.size(100.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.common_rhythm),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            if (appMode == "STREAMING") {
                                Text(
                                    text = stringResource(R.string.splashscreen_go),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isAuthenticated) {
                            "Ready to connect"
                        } else {
                            "Authentication pending"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                            onSwitchToLocalMode()
                        }
                    ) {
                        Text(text = stringResource(R.string.streaminghomescreen_leave_go_mode))
                    }
                }
            }

            item {
                val settingsItems = remember(displaySelectedService, serviceSessions) {
                    StreamingServiceOptions.defaults.map { service ->
                        val isSelected = displaySelectedService == service.id
                        val isConnected = serviceSessions[service.id]?.isConnected == true
                        
                        Material3SettingsItem(
                            leadingContent = {
                                val providerIconRes = when (service.id) {
                                    StreamingServiceId.SUBSONIC -> R.drawable.ic_subsonic
                                    StreamingServiceId.JELLYFIN -> R.drawable.ic_jellyfin
                                    else -> null
                                }
                                if (providerIconRes != null) {
                                    Icon(
                                        painter = painterResource(id = providerIconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = stringResource(id = service.nameRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            description = {
                                Text(
                                    text = stringResource(id = service.descriptionRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (isSelected) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                icon = RhythmIcons.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = stringResource(id = R.string.streaming_selected),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            appSettings.setStreamingService(service.id)
                                            onConfigureService(service.id)
                                        }
                                    ) {
                                        Text(
                                            text = if (isConnected) {
                                                stringResource(id = R.string.streaming_manage)
                                            } else {
                                                stringResource(id = R.string.streaming_connect)
                                            }
                                        )
                                    }
                                }
                            },
                            onClick = {
                                appSettings.setStreamingService(service.id)
                                onConfigureService(service.id)
                            }
                        )
                    }
                }

                Material3SettingsGroup(
                    items = settingsItems
                )
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

private fun selectedServiceLabel(selectedService: String, context: android.content.Context): String {
    if (selectedService.isBlank()) return context.getString(R.string.streaming_not_selected)
    val matching = StreamingServiceOptions.defaults.firstOrNull { it.id == selectedService }
    return matching?.let { context.getString(it.nameRes) } ?: context.getString(R.string.streaming_not_selected)
}
