package com.routina.glance.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.glance.R
import com.routina.glance.data.RawEvent
import java.time.Instant
import java.time.ZoneId

/** 開發者頁：原始通知紀錄。點一筆展開全文，右上角分享成純文字 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(viewModel: GlanceViewModel, onBack: () -> Unit) {
    val events by viewModel.rawEvents.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { share(context, events) }, enabled = events.isNotEmpty()) {
                        Icon(Icons.Default.Share, stringResource(R.string.action_share))
                    }
                    IconButton(onClick = { confirmClear = true }, enabled = events.isNotEmpty()) {
                        Icon(Icons.Default.DeleteSweep, stringResource(R.string.action_clear))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            item { HintText(stringResource(R.string.debug_hint), Modifier.padding(16.dp)) }
            if (events.isEmpty()) {
                item { HintText(stringResource(R.string.debug_empty), Modifier.padding(horizontal = 16.dp)) }
            }
            items(events, key = { it.id }) { event ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = if (expanded == event.id) null else event.id }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "${timestamp(event.at)}  ${event.kind}  ${event.packageName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = event.body,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = if (expanded == event.id) Int.MAX_VALUE else 4
                    )
                }
                HorizontalDivider()
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.debug_clear_title),
            confirmLabel = stringResource(R.string.action_clear),
            onConfirm = { viewModel.clearRawEvents() },
            onDismiss = { confirmClear = false }
        )
    }
}

private fun timestamp(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().toString()

/**
 * 分享成純文字。Intent 走 binder，整包太大（約 1MB，字串以 UTF-16 計）會直接失敗，
 * 所以從最新的往回塞，塞到上限就停。
 */
private fun share(context: Context, events: List<RawEvent>) {
    val text = StringBuilder()
    for (event in events) {
        val entry = "=== ${timestamp(event.at)} ${event.kind} ${event.packageName}\n${event.body}\n"
        if (text.length + entry.length > MAX_SHARE_CHARS) break
        text.append(entry)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "Routina Glance raw notifications")
        .putExtra(Intent.EXTRA_TEXT, text.toString())
    runCatching { context.startActivity(Intent.createChooser(send, null)) }
}

private const val MAX_SHARE_CHARS = 100_000
