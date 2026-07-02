package com.douyin.downloaderqh.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

/**
 * Creates a backdrop that captures the wallpaper + content underneath.
 * Call this at the root level, then pass [backdrop] to glass elements.
 */
@Composable
fun rememberGlassBackdrop(
    backgroundColor: Color = MaterialTheme.colorScheme.surface
): Backdrop {
    return rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
}

/**
 * Glass-styled card. Uses semi-transparent surface (not actual drawBackdrop
 * since cards live inside the content layer). Visually consistent with
 * the glass bottom bar / top bar.
 */
@Composable
fun GlassCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (onClick != null) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    }

    Card(
        onClick = { onClick?.invoke() },
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        content = content
    )
}

/**
 * Non-clickable glass card variant.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        ),
        content = content
    )
}

/**
 * Glass-styled bottom navigation bar.
 * Renders over the backdrop with blur + lens effects.
 *
 * @param backdrop The backdrop capturing the wallpaper + content underneath
 * @param tabs List of tab definitions (label, icon, badge count)
 * @param selectedIndex Currently selected tab index
 * @param onTabClick Tab click callback
 * @param modifier Modifier
 */
@Composable
fun GlassBottomBar(
    backdrop: Backdrop,
    tabs: List<BottomTabDef>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) },
                effects = {
                    blur(6.dp.toPx())
                    lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
                },
                onDrawSurface = {
                    drawRect(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    )
                }
            )
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onTabClick(index) },
                    icon = {
                        if (tab.badgeCount > 0) {
                            BadgedBox(badge = { Badge { Text("${tab.badgeCount}") } }) {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        } else {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        unselectedIconTintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

data class BottomTabDef(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

/**
 * Glass-styled top app bar.
 * Uses the same backdrop for a consistent glass look.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    backdrop: Backdrop,
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) },
                effects = {
                    blur(6.dp.toPx())
                    lens(16.dp.toPx(), 32.dp.toPx(), depthEffect = true)
                },
                onDrawSurface = {
                    drawRect(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                }
            )
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = { navigationIcon?.invoke() },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

/**
 * Glass-styled input field container.
 * Wraps content with a glass background for text fields.
 */
@Composable
fun GlassInputContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .fillMaxWidth()
            .then(
                Modifier
                    .fillMaxWidth()
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            content()
        }
    }
}
