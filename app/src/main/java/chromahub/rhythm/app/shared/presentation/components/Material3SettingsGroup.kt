package chromahub.rhythm.app.shared.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

/**
 * Material 3 Expressive settings group — card stack with dynamic corner radii.
 *
 * Single item → fully rounded 24dp
 * First item → 24dp top, 6dp bottom
 * Middle items → 6dp all
 * Last item → 6dp top, 24dp bottom
 *
 * When [itemShape] is provided it overrides the corner radii for every card in
 * the group, letting the group blend into a larger connected card stack. When
 * [lastItemShape] is provided it overrides only the last card (taking precedence
 * over [itemShape]) so the group can cap the bottom of a connected stack.
 */
@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    itemShape: Shape? = null,
    lastItemShape: Shape? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 18.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, item ->
                val isLast = index == items.size - 1
                val shape = when {
                    lastItemShape != null && isLast -> lastItemShape
                    itemShape != null -> itemShape
                    items.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(
                        topStart = 24.dp, topEnd = 24.dp,
                        bottomStart = 6.dp, bottomEnd = 6.dp
                    )
                    isLast -> RoundedCornerShape(
                        topStart = 6.dp, topEnd = 6.dp,
                        bottomStart = 24.dp, bottomEnd = 24.dp
                    )
                    else -> RoundedCornerShape(6.dp)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Material3SettingsItemRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun Material3SettingsItemRow(item: Material3SettingsItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val defaultIconBg = if (item.isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val defaultIconTint = if (item.isHighlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    // Expressive press scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "setting_item_scale"
    )

    // Animated icon background color
    val iconBgColor by animateColorAsState(
        targetValue = when {
            !item.enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
            isPressed -> (item.iconBackgroundTint ?: defaultIconBg).copy(alpha = 0.86f)
            else -> (item.iconBackgroundTint ?: defaultIconBg)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_bg_color"
    )

    // Animated icon color
    val iconColor by animateColorAsState(
        targetValue = if (!item.enabled)
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else
            (item.iconTint ?: defaultIconTint),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = item.enabled && item.onClick != null,
                interactionSource = interactionSource,
                indication = null,
                onClick = { item.onClick?.invoke() }
            )
            .padding(horizontal = 21.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with circle background or custom leadingContent
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(modifier = Modifier.width(16.dp))
        } else item.icon?.let { icon ->
            Surface(
                modifier = Modifier.size(40.dp),
                shape = item.iconShape ?: CircleShape,
                color = iconBgColor,
                tonalElevation = if (item.isHighlighted) 2.dp else 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (icon) {
                        is MaterialSymbolIcon -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        is ImageVector -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        // Title + description
        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    color = if (!item.enabled)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                item.title()
            }

            val scope = item.scope
            if (scope != SettingScope.BOTH) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = when (scope) {
                        SettingScope.LOCAL -> MaterialTheme.colorScheme.secondaryContainer
                        SettingScope.STREAMING -> MaterialTheme.colorScheme.primaryContainer
                        SettingScope.BOTH -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = when (scope) {
                            SettingScope.LOCAL -> "Local"
                            SettingScope.STREAMING -> "Streaming"
                            SettingScope.BOTH -> "Both"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = when (scope) {
                            SettingScope.LOCAL -> MaterialTheme.colorScheme.onSecondaryContainer
                            SettingScope.STREAMING -> MaterialTheme.colorScheme.onPrimaryContainer
                            SettingScope.BOTH -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = if (!item.enabled)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    desc()
                }
            }
        }

        // Trailing content (switch, chevron, etc.)
        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Data class for a Material 3 Expressive settings item.
 */
data class Material3SettingsItem(
    val icon: Any? = null,
    val iconTint: Color? = null,
    val iconBackgroundTint: Color? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val isHighlighted: Boolean = false,
    val iconShape: Shape? = null,
    val enabled: Boolean = true,
    val scope: SettingScope = SettingScope.BOTH,
    val leadingContent: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

enum class SettingScope {
    LOCAL,
    STREAMING,
    BOTH
}
