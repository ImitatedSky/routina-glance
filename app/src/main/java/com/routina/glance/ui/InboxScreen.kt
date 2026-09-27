package com.routina.glance.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.glance.R
import com.routina.glance.data.Conversation
import com.routina.glance.data.SearchHit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: GlanceViewModel,
    onOpenConversation: (conversationId: Long, focusMessageId: Long?) -> Unit,
    onOpenSettings: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val accessEnabled by viewModel.accessEnabled.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    // 打字時停 300ms 才查，不必每個字都查一次資料庫
    LaunchedEffect(query, searching) {
        if (!searching) return@LaunchedEffect
        delay(300)
        viewModel.search(query)
    }

    fun closeSearch() {
        searching = false
        query = ""
        viewModel.clearSearch()
    }
    BackHandler(enabled = searching) { closeSearch() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        SearchField(query = query, onQueryChange = { query = it })
                    } else {
                        Text(stringResource(R.string.inbox_title), style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    if (searching) {
                        IconButton(onClick = { closeSearch() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_close_search))
                        }
                    }
                },
                actions = {
                    if (!searching) {
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Default.Search, stringResource(R.string.action_search))
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, stringResource(R.string.action_settings))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val hits = results
        if (searching && hits != null) {
            SearchResults(
                hits = hits,
                contentPadding = padding,
                onOpen = { hit -> onOpenConversation(hit.conversationId, hit.messageId) }
            )
            return@Scaffold
        }

        // 篩選晶片只列出真的有對話的 App
        val packages = conversations.map { it.packageName }.distinct()
        val shown = if (filter == null) conversations else conversations.filter { it.packageName == filter }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            if (!accessEnabled) {
                item { AccessCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            }
            if (packages.size > 1) {
                item {
                    FilterRow(
                        packages = packages,
                        selected = filter,
                        onSelect = { viewModel.setFilter(it) }
                    )
                }
            }
            items(shown, key = { it.id }) { conversation ->
                ConversationRow(
                    conversation = conversation,
                    onClick = { onOpenConversation(conversation.id, null) }
                )
            }
            if (conversations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (accessEnabled) HintText(stringResource(R.string.inbox_empty))
                        CaveatsCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.inbox_search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions.Default,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
    )
}

@Composable
private fun FilterRow(packages: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.inbox_filter_all)) }
        )
        packages.forEach { pkg ->
            val label = remember(pkg) { appLabel(context, pkg) }
            FilterChip(
                selected = selected == pkg,
                onClick = { onSelect(if (selected == pkg) null else pkg) },
                label = { Text(label) },
                leadingIcon = { AppIcon(pkg, size = 18.dp) }
            )
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppIcon(conversation.packageName)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = listTime(conversation.lastMessageAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.lastPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.unseenCount > 0) {
                    Badge(modifier = Modifier.padding(start = 8.dp)) {
                        Text(conversation.unseenCount.coerceAtMost(999).toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(hits: List<SearchHit>, contentPadding: PaddingValues, onOpen: (SearchHit) -> Unit) {
    val self = stringResource(R.string.inbox_self)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        if (hits.isEmpty()) {
            item { HintText(stringResource(R.string.inbox_search_empty), Modifier.padding(16.dp)) }
        }
        items(hits, key = { it.messageId }) { hit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(hit) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIcon(hit.packageName, size = 32.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = hit.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = hitTime(hit.postedAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val sender = if (hit.isSelf) self else hit.sender
                    Text(
                        text = if (sender.isBlank() || sender == hit.title) hit.text else "$sender：${hit.text}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
