package com.routina.glance

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.routina.glance.ui.ConversationScreen
import com.routina.glance.ui.DebugScreen
import com.routina.glance.ui.GlanceViewModel
import com.routina.glance.ui.InboxScreen
import com.routina.glance.ui.SettingsScreen
import com.routina.glance.ui.theme.GlanceTheme

/**
 * 繼承 FragmentActivity 而不是 ComponentActivity：BiometricPrompt 需要掛在 Fragment 上。
 * FragmentActivity 本身就是 ComponentActivity 的子類，Compose 照用。
 */
class MainActivity : FragmentActivity() {

    private val viewModel: GlanceViewModel by viewModels()
    private val settings get() = (application as GlanceApp).repository.settings

    /** 目前是否上鎖。每次從背景回來（onStop 之後）都會重新鎖上 */
    private var locked by mutableStateOf(false)

    /** 驗證畫面開著的時候不要再開第二個；PIN 畫面是另一個 Activity，會讓這裡走 onStop */
    private var authenticating = false

    /** 回到前景時要不要自動跳出驗證。使用者按取消之後就不要一直跳，改按「解鎖」 */
    private var promptOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (settings.current.appLock) {
            locked = true
            promptOnResume = true
        }
        setContent {
            GlanceTheme {
                val current by viewModel.settings.collectAsStateWithLifecycle()
                // 開了 App 鎖就不讓系統在「最近使用」裡顯示畫面內容，也擋截圖
                LaunchedEffect(current.appLock) {
                    if (current.appLock) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 鎖畫面蓋在上面而不是換掉底下的畫面：解鎖後回到原本看的那個對話
                    Box {
                        GlanceRoot(viewModel)
                        if (locked) {
                            BackHandler { finish() }
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                LockScreen(onUnlock = { unlock() })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 權限可能是剛去系統設定開的；開了卻沒綁上的話順便請系統重新綁定
        viewModel.refreshAccess()
        if (locked && promptOnResume) {
            promptOnResume = false
            unlock()
        }
    }

    override fun onStop() {
        super.onStop()
        if (settings.current.appLock && !authenticating && !isChangingConfigurations) {
            locked = true
            promptOnResume = true
        }
    }

    private fun unlock() {
        if (authenticating) return
        // 使用者後來把螢幕鎖定拿掉了：沒有東西可以驗證，直接放行，免得永遠進不去
        if (!AppLock.canAuthenticate(this)) {
            locked = false
            return
        }
        authenticating = true
        AppLock.prompt(this) { ok ->
            authenticating = false
            if (ok) locked = false
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
        Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge)
        Button(onClick = onUnlock) { Text(stringResource(R.string.lock_unlock)) }
    }
}

private object Routes {
    const val INBOX = "inbox"
    const val SETTINGS = "settings"
    const val DEBUG = "debug"
    const val CONVERSATION = "conversation/{id}?focus={focus}"

    const val ARG_ID = "id"
    const val ARG_FOCUS = "focus"

    /** focus 是搜尋點進來時要捲到的那一則，-1 = 沒有 */
    fun conversation(id: Long, focus: Long?) = "conversation/$id?focus=${focus ?: -1}"
}

@Composable
private fun GlanceRoot(viewModel: GlanceViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.INBOX) {
        composable(Routes.INBOX) {
            InboxScreen(
                viewModel = viewModel,
                onOpenConversation = { id, focus -> navController.navigate(Routes.conversation(id, focus)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(
                navArgument(Routes.ARG_ID) { type = NavType.LongType },
                navArgument(Routes.ARG_FOCUS) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val focus = entry.arguments?.getLong(Routes.ARG_FOCUS) ?: -1L
            ConversationScreen(
                viewModel = viewModel,
                conversationId = entry.arguments?.getLong(Routes.ARG_ID) ?: 0L,
                focusMessageId = focus.takeIf { it >= 0 },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onOpenDebug = { navController.navigate(Routes.DEBUG) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DEBUG) {
            DebugScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
