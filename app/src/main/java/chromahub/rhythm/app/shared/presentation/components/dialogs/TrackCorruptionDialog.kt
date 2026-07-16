package chromahub.rhythm.app.shared.presentation.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

@Composable
fun TrackCorruptionDialog(
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    trackName: String,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                icon = RhythmIcons.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                size = 28.dp
            )
        },
        title = {
            Text(
                text = "Track Playback Error",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "The song \"$trackName\" encountered a playback issue.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = errorMessage ?: "It appears to be corrupted or unplayable. Would you like to skip to the next track?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onSkip()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Skip Song")
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = {},
        shape = RoundedCornerShape(28.dp)
    )
}
