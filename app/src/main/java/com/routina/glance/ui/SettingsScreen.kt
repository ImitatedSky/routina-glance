package com.routina.glance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.glance.AppLock
import com.routina.glance.R
import com.routina.glance.capture.KnownApps
import kotlinx.coroutines.launch

/** 保存期限的選項，0 = 永久 */
private val RETENTION_OPTIONS = listOf(7, 30, 90, 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GlanceViewModel,
    onOpenDebug: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accessEnabled by viewModel.accessEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }

    val lockUnavailable = stringResource(R.string.settings_app_lock_unavailable)
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    // 內建的五個固定排前面；其他加進來過的接在後面，關掉了也留著，才開得回來
    val apps = KnownApps.defaults + (settings.extraApps + settings.monitored - KnownApps.defaults.toSet()).sorted()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- 通知存取 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_access),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(if (accessEnabled) R.string.settings_access_on else R.string.settings_access_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (accessEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            if (!accessEnabled) {
                AccessCard()
            } else {
                OutlinedButton(onClick = { openListenerSettings(context) }) {
                    Text(stringResource(R.string.access_open))
                }
            }

            HorizontalDivider()

            // ---- 監聽的 App ----
            Text(stringResource(R.string.settings_apps), style = MaterialTheme.typography.titleMedium)
            HintText(stringResource(R.string.settings_apps_hint))
            apps.forEach { pkg ->
                val label = remember(pkg) { appLabel(context, pkg) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppIcon(pkg, size = 32.dp)
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = pkg in settings.monitored,
                        onCheckedChange = { viewModel.setMonitored(pkg, it) }
                    )
                }
            }
            TextButton(onClick = { picking = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.settings_add_app), modifier = Modifier.padding(start = 6.dp))
            }

            HorizontalDivider()

            // ---- 保存期限 ----
            Text(stringResource(R.string.settings_retention), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RETENTION_OPTIONS.forEach { days ->
                    FilterChip(
                        selected = settings.retentionDays == days,
                        onClick = { viewModel.setRetention(days) },
                        label = {
                            Text(
                                if (days == 0) stringResource(R.string.settings_retention_forever)
                                else stringResource(R.string.settings_retention_days, days)
                            )
                        }
                    )
                }
            }
            HintText(stringResource(R.string.settings_retention_hint))

            HorizontalDivider()

            // ---- 清掉原通知 ----
            SwitchRow(
                label = stringResource(R.string.settings_clear_original),
                checked = settings.clearOriginal,
                onChange = { viewModel.setClearOriginal(it) }
            )
            HintText(stringResource(R.string.settings_clear_original_hint))

            // ---- App 鎖 ----
            SwitchRow(
                label = stringResource(R.string.settings_app_lock),
                checked = settings.appLock,
                onChange = { on ->
                    // 手機沒設螢幕鎖定就開不了，否則會把自己鎖在外面
                    if (on && !AppLock.canAuthenticate(context)) {
                        scope.launch { snackbar.showSnackbar(lockUnavailable) }
                    } else {
                        viewModel.setAppLock(on)
                    }
                }
            )
            HintText(stringResource(R.string.settings_app_lock_hint))

            HorizontalDivider()

            CaveatsCard()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDebug)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_debug),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }

            HintText(stringResource(R.string.settings_privacy))
            HintText(stringResource(R.string.settings_version, versionName))
        }
    }

    if (picking) {
        AppPickerDialog(
            viewModel = viewModel,
            exclude = apps.toSet(),
            onPick = { viewModel.addApp(it) },
            onDismiss = { picking = false }
        )
    }
}

/** 「新增其他 App」：列出有桌面圖示、還不在清單上的 App */
@Composable
private fun AppPickerDialog(
    viewModel: GlanceViewModel,
    exclude: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apps by remember { mutableStateOf<List<LauncherApp>?>(null) }
    LaunchedEffect(Unit) { apps = viewModel.launcherApps() }
    val choices = apps?.filter { it.packageName !in exclude }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_pick_app), style = MaterialTheme.typography.headlineSmall) },
        text = {
            when {
                choices == null -> Unit
                choices.isEmpty() -> HintText(stringResource(R.string.settings_pick_app_empty))
                else -> LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(choices, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPick(app.packageName)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIcon(app.packageName, size = 32.dp)
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
