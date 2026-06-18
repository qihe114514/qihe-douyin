package com.douyin.downloaderqh.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.douyin.downloaderqh.ui.HistoryEntry

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    historyList: List<HistoryEntry>,
    onItemClick: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showSubMenu: String? by remember { mutableStateOf(null) }
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("解析历史", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectMode) { isSelectMode = false; selectedItems = emptySet() }
                        else onBack()
                    }) {
                        Icon(if (isSelectMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.ImportExport, "导入导出")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("导入") }, onClick = { showMenu = false; showSubMenu = "import" })
                                DropdownMenuItem(text = { Text("导出") }, onClick = { showMenu = false; showSubMenu = "export" })
                            }
                        }
                        if (isSelectMode) {
                            TextButton(onClick = {
                                if (selectedItems.size == historyList.size) selectedItems = emptySet()
                                else selectedItems = historyList.indices.toSet()
                            }) {
                                Text(if (selectedItems.size == historyList.size) "取消全选" else "全选")
                            }
                            IconButton(onClick = {
                                selectedItems = emptySet(); isSelectMode = false; onClearHistory()
                            }) { Icon(Icons.Default.Delete, "删除选中") }
                        } else {
                            IconButton(onClick = { isSelectMode = true; selectedItems = emptySet() }) {
                                Icon(Icons.Default.Delete, "删除")
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (showSubMenu == "import") {
            AlertDialog(
                onDismissRequest = { showSubMenu = null },
                title = { Text("从剪贴板导入") },
                text = { Text("将剪贴板中的链接导入到历史记录") },
                confirmButton = {
                    TextButton(onClick = {
                        showSubMenu = null
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (text.isNotBlank()) {
                            val urls = text.lines().mapNotNull { it.trim().takeIf { u -> u.startsWith("http") } }
                            urls.forEach { url ->
                                // 通过 onItemClick 触发解析添加历史
                                onItemClick(HistoryEntry(url = url, title = url, type = "imported", timestamp = System.currentTimeMillis()))
                            }
                            Toast.makeText(context, "已导入 ${urls.size} 条记录", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "剪贴板为空或不是链接", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("导入") }
                },
                dismissButton = { TextButton(onClick = { showSubMenu = null }) { Text("取消") } }
            )
        }
        if (showSubMenu == "export") {
            AlertDialog(
                onDismissRequest = { showSubMenu = null },
                title = { Text("导出到剪贴板") },
                text = { Text("将所有历史记录链接复制到剪贴板") },
                confirmButton = {
                    TextButton(onClick = {
                        showSubMenu = null
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val content = historyList.joinToString("\n") { it.url }
                        clipboard.setPrimaryClip(ClipData.newPlainText("history", content))
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }) { Text("导出") }
                },
                dismissButton = { TextButton(onClick = { showSubMenu = null }) { Text("取消") } }
            )
        }

        if (historyList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("暂无解析历史", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
            ) {
                itemsIndexed(historyList) { index, entry ->
                    val isSelected = selectedItems.contains(index)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).combinedClickable(
                            onClick = {
                                if (isSelectMode) {
                                    selectedItems = if (isSelected) selectedItems - index else selectedItems + index
                                } else {
                                    onItemClick(entry)
                                }
                            },
                            onLongClick = {
                                if (!isSelectMode) { isSelectMode = true; selectedItems = setOf(index) }
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (isSelectMode) {
                                Checkbox(checked = isSelected, onCheckedChange = {
                                    selectedItems = if (isSelected) selectedItems - index else selectedItems + index
                                })
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = entry.title.ifBlank { "未知" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = entry.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
