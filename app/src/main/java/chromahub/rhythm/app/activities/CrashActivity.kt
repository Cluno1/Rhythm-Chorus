package chromahub.rhythm.app.activities

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import chromahub.rhythm.app.ui.theme.RhythmTheme
import kotlin.system.exitProcess
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import chromahub.rhythm.app.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.EaseInOut
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource

class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashLog = intent.getStringExtra(EXTRA_CRASH_LOG)

        setContent {
            RhythmTheme {
                CrashScreen(crashLog = crashLog)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CrashScreen(crashLog: String?) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // Responsive sizing
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600
        val contentMaxWidth = 600.dp
        val cardPadding = if (isTablet) 32.dp else 28.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    horizontal = if (isTablet) 24.dp else 0.dp,
                    vertical = if (isTablet) 24.dp else 0.dp
                ),
            contentAlignment = if (isTablet) Alignment.Center else Alignment.TopCenter
        ) {
            // Crash card container
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 0.dp,
                modifier = if (isTablet) {
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = contentMaxWidth)
                } else {
                    Modifier.fillMaxSize()
                }
                    .then(if (isTablet) Modifier else Modifier.fillMaxHeight())
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = cardPadding,
                            end = cardPadding,
                            top = cardPadding * 2,
                            bottom = cardPadding
                        ),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Bug icon centered at top, onboarding style
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = RhythmIcons.BugReport,
                                contentDescription = stringResource(R.string.crash_bug_report),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    // Left-aligned texts
                    Text(
                        text = stringResource(R.string.crashactivity_uh_oh_looks_like), // Comic text
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = stringResource(R.string.crashactivity_dont_fret_our_app), // Comic text
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Crash log text field
                    OutlinedTextField(
                        value = crashLog ?: "No funny business here, just a crash log!", // Comic text
                        onValueChange = { /* Read-only */ },
                        label = { Text(stringResource(R.string.crashactivity_secret_crash_scrolls)) }, // Comic label
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                            .padding(bottom = 40.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // App logo and name at center
                    val infiniteTransition = rememberInfiniteTransition(label = "crash_animations")
                    val logoGlow by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "logoGlow"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 48.dp)
                    ) {
                        // App logo with glowing effect
                        AnimatedVisibility(
                            visible = true,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(
                                animationSpec = tween(1000)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.rhythm_splash_logo),
                                    contentDescription = stringResource(R.string.updates_rhythm_logo_cd),
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        // App name
                        AnimatedVisibility(
                            visible = true,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
                        ) {
                            Text(
                                text = stringResource(R.string.cd_rhythm_splash),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Onboarding-style buttons at bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share button (replaces back button)
                        val shareButtonScale = remember { Animatable(1f) }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    shareButtonScale.animateTo(0.92f, animationSpec = tween(100))
                                    shareButtonScale.animateTo(1f, animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessHigh
                                    ))
                                }
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Rhythm App Crash Log:\n\n$crashLog")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Crash Log"))
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = shareButtonScale.value
                                    scaleY = shareButtonScale.value
                                },
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.crashactivity_share), style = MaterialTheme.typography.labelLarge)
                        }

                        // Restart button (styled like onboarding)
                        val restartButtonScale = remember { Animatable(1f) }
                        Button(
                            onClick = {
                                scope.launch {
                                    restartButtonScale.animateTo(0.92f, animationSpec = tween(100))
                                    restartButtonScale.animateTo(1f, animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessHigh
                                    ))
                                }
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                exitProcess(0)
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = restartButtonScale.value
                                    scaleY = restartButtonScale.value
                                },
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Text(stringResource(R.string.crash_restart_app), style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = RhythmIcons.Forward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                }
            }
        }
    }

    companion object {
        const val EXTRA_CRASH_LOG = "extra_crash_log"

        fun start(context: Context, crashLog: String) {
            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra(EXTRA_CRASH_LOG, crashLog)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewCrashScreen() {
        RhythmTheme {
            CrashScreen(crashLog = "Sample crash log details here.\nAnother line of log.")
        }
    }
}
