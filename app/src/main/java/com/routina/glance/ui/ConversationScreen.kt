package com.routina.glance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.routina.glance.R
import com.routina.glance.data.Message
import java.time.LocalDate

/** 對話裡的一列：日期分隔線或一則訊息 */
private sealed interface ChatRow {
    data class Day(val date: LocalDate) : ChatRow
    data class Bubble(val message: Message) : ChatRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: GlanceViewModel,
    conversationId: Long,
    focusMessageId: Long?,
    onBack: () -> Unit
) {
    val conversation by remember(conversationId) { viewModel.conversation(conversationId) }.collectAsState(null)
    val messages by remember(conversationId) { viewModel.messages(conversationId) }.collectAsState(null)
    var confirmDelete by remember { mutableStateOf(false) }

    // 開著這個對話時新進來的訊息也算看過，所以跟著訊息數重設
    LaunchedEffect(conversationId, messages?.size) {
        viewModel.markSeen(conversationId)
    }

    val rows = remember(messages) {
        buildList {
            var lastDay: LocalDate? = null
            messages.orEmpty().forEach { m ->
                val day = localDate(m.postedAt)
                if (day != lastDay) add(ChatRow.Day(day))
                lastDay = day
                add(ChatRow.Bubble(m))
            }
        }
    }

    // 第一次載入時捲到底（最新）；從搜尋進來則捲到那一則
    val listState = rememberLazyListState()
    var scrolled by rememberSaveable(conversationId) { mutableStateOf(false) }
    var previousCount by remember { mutableStateOf(0) }
    LaunchedEffect(rows) {
        if (rows.isEmpty()) return@LaunchedEffect
        val wasAtBottom = (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= previousCount - 1
        previousCount = rows.size
        if (!scrolled) {
            val target = focusMessageId
                ?.let { id -> rows.indexOfFirst { it is ChatRow.Bubble && it.message.id == id } }
                ?.takeIf { it >= 0 }
                ?: rows.lastIndex
            listState.scrollToItem(target)
            scrolled = true
        } else if (wasAtBottom) {
            // 本來就停在最底下時，新訊息進來跟著往下
            listState.animateScrollToItem(rows.lastIndex)
        }
    }

    val isGroup = conversation?.isGroup == true
    val title = conversation?.title.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    conversation?.let { AppIcon(it.packageName, size = 24.dp) }
                    OverflowMenu(
                        listOf(stringResource(R.string.conversation_delete) to { confirmDelete = true })
                    )
                }
            )
        }
    ) { padding ->
        val today = stringResource(R.string.conversation_today)
        val yesterday = stringResource(R.string.conversation_yesterday)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(count = rows.size, key = { index ->
                when (val row = rows[index]) {
                    is ChatRow.Day -> "day-${row.date}"
                    is ChatRow.Bubble -> row.message.id
                }
            }) { index ->
                when (val row = rows[index]) {
                    is ChatRow.Day -> DaySeparator(dayLabel(row.date, today, yesterday))
                    is ChatRow.Bubble -> {
                        // 群組裡同一個人連續傳，只在第一則標名字
                        val previous = (rows.getOrNull(index - 1) as? ChatRow.Bubble)?.message
                        val showSender = isGroup && !row.message.isSelf &&
                            previous?.sender != row.message.sender
                        Bubble(
                            message = row.message,
                            showSender = showSender,
                            highlighted = row.message.id == focusMessageId
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.conversation_delete_title),
            message = stringResource(R.string.conversation_delete_message),
            onConfirm = {
                viewModel.deleteConversation(conversationId)
                onBack()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun DaySeparator(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 3.dp)
        )
    }
}

/** 自己傳的靠右、用主色容器；別人傳的靠左、用卡片色 */
@Composable
private fun Bubble(message: Message, showSender: Boolean, highlighted: Boolean) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        highlighted -> colors.tertiaryContainer
        message.isSelf -> colors.primaryContainer
        else -> colors.surface
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isSelf) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (message.isSelf) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (showSender) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Column(
                modifier = Modifier
                    .background(background, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = message.text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = clockTime(message.postedAt) +
                        if (message.fromFallback) " · " + stringResource(R.string.conversation_guess_short) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
