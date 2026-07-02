package com.douyin.downloaderqh.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Glass-styled card. Uses semi-transparent surface for a frosted glass look.
 */
@Composable
fun GlassCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = { onClick?.invoke() },
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
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
 * Uses semi-transparent surface with glass-like colors.
 */
@Composable
fun GlassBottomBar(
    tabs: List<BottomTabDef>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
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
 * Uses semi-transparent surface for a frosted glass look.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
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
