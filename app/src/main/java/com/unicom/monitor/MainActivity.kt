package com.unicom.monitor

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 33) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent { MaterialTheme { AppScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { SettingsRepository(ctx) }
    var settings by remember { mutableStateOf(Settings()) }
    val cards = remember { mutableStateListOf<BalanceData>() }
    var showSettings by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { repo.settings.collect { settings = it } }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val file = File(ctx.filesDir, "emoji_image")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                settings = settings.copy(imagePath = file.absolutePath)
                scope.launch { repo.save(settings) }
                message = "表情包图片已保存"
            } catch (e: Exception) { message = "图片保存失败: ${e.message}" }
        }
    }

    val pickBg = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val file = File(ctx.filesDir, "chat_bg")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                settings = settings.copy(bgPath = file.absolutePath)
                scope.launch { repo.save(settings) }
                message = "背景已设置"
            } catch (e: Exception) { message = "背景设置失败: ${e.message}" }
        }
    }

    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val file = File(ctx.filesDir, "avatar_image")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                settings = settings.copy(avatarPath = file.absolutePath)
                scope.launch { repo.save(settings) }
                message = "头像已设置"
            } catch (e: Exception) { message = "头像设置失败: ${e.message}" }
        }
    }

    fun refresh() {
        if (settings.cookie.isBlank()) { message = "请先在右上角设置里填写 cookie"; showSettings = true; return }
        loading = true; message = ""
        scope.launch {
            try {
                val d = UnicomApi.fetchBalance(settings.cookie, settings.mobile)
                cards.add(0, d)
                if (cards.size > 20) cards.removeRange(20, cards.size)
                NotificationHelper.notify(ctx, "联通余量", NotificationHelper.buildText(d, settings.emojiSet), settings.imagePath)
                message = "刷新成功"
            } catch (e: Exception) { message = "失败: ${e.message}" }
            finally { loading = false }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (settings.bgPath.isNotEmpty()) {
            val bg = remember(settings.bgPath) { BitmapFactory.decodeFile(settings.bgPath) }
            if (bg != null) {
                Image(bg.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            var inputText by remember { mutableStateOf(settings.customText) }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入卡片底部文字") },
                    singleLine = true
                )
                Button(onClick = {
                    settings = settings.copy(customText = inputText)
                    scope.launch { repo.save(settings) }
                    message = "已保存"
                }) { Text("保存") }
            }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val topAvatar = remember(settings.avatarPath) {
                            if (settings.avatarPath.isNotEmpty()) BitmapFactory.decodeFile(settings.avatarPath) else null
                        }
                        if (topAvatar != null) {
                            Image(topAvatar.asImageBitmap(), null, Modifier.size(34.dp).clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(Color(0xFF2196F3)), contentAlignment = Alignment.Center) {
                                Text("联", color = Color.White, fontSize = 15.sp)
                            }
                        }
                        Text("联通流量监控")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                if (message.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth().padding(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Text(message, fontSize = 13.sp, color = Color(0xFFD84315), modifier = Modifier.padding(10.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (cards.isEmpty()) {
                    Card(Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("还没有数据", fontWeight = FontWeight.Bold)
                            Text("点底部按钮查询，或在右上角设置里填写 cookie", fontSize = 13.sp)
                        }
                    }
                }
                cards.forEach { d ->
                    BalanceCard(d, settings.emojiSet, settings.avatarPath, resolveTextColor(settings.textColor), settings.customText)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(70.dp))
            }

        }
    }

    if (message.isNotEmpty()) {
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(8000)
            message = ""
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            repo = repo,
            onUpdate = { settings = it },
            onPickImage = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPickBg = { pickBg.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPickAvatar = { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onClose = { showSettings = false },
            ctx = ctx,
        )
    }
    }
}

@Composable
fun BalanceCard(d: BalanceData, emojiSet: String, avatarPath: String, textColor: Color = Color.Black, customText: String = "") {
    val e = NotificationHelper.emojis(emojiSet)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 头像
        val avatarBmp = remember(avatarPath) {
            if (avatarPath.isNotEmpty()) BitmapFactory.decodeFile(avatarPath) else null
        }
        if (avatarBmp != null) {
            Image(
                bitmap = avatarBmp.asImageBitmap(),
                contentDescription = "头像",
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(50)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(50)).background(Color(0xFF2196F3)),
                contentAlignment = Alignment.Center
            ) { Text("联", color = Color.White, fontSize = 18.sp) }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f))
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(d.packageName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                Spacer(Modifier.height(4.dp))
                Text("${e[0]} 跳 ➠ ${d.unsharedFlowMb}M", fontSize = 15.sp, color = textColor)
                Text("${e[1]} 免 ➠ ${d.freeFlowGb}G", fontSize = 15.sp, color = textColor)
                Text("${e[2]} 总流量 ➠ ${"%.2f".format(d.totalGb)}G", fontSize = 15.sp, color = textColor)
                Text("${e[3]} 已消耗 ➠ ${"%.2f".format(d.usedGb)}G", fontSize = 15.sp, color = textColor)
                Text("${e[4]} 免流已用 ➠ ${"%.2f".format(d.freeUsedGb)}G", fontSize = 15.sp, color = textColor)
                Text("${e[5]} 今日跳量 ➠ ${d.dailyMb}M", fontSize = 15.sp, color = textColor)
                Text("${e[6]} 剩余流量 ➠ ${"%.2f".format(d.remainGb)}G", fontSize = 15.sp, color = textColor)
                Text("————————————————", fontSize = 13.sp, color = Color.Gray)
                Text("${e[7]} 联通时间 ➠ ${d.updateTime}", fontSize = 15.sp, color = textColor)
                Spacer(Modifier.height(3.dp))
                Text("💰 剩余话费 ➠ ${d.fee}元", fontSize = 14.sp, color = textColor)
                Text("📞 剩余语音 ➠ ${d.voice}分钟", fontSize = 14.sp, color = textColor)
                Text("⭐ 可用积分 ➠ ${d.point}", fontSize = 14.sp, color = textColor)
                Spacer(Modifier.height(4.dp))
                Text(customText.ifBlank { quotes.random() }, fontSize = 12.sp, color = Color.Gray)
                Text(d.updateTime, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

fun resolveTextColor(name: String): Color = when (name) {
    "white" -> Color.White
    "gray" -> Color(0xFF37474F)
    "blue" -> Color(0xFF0D47A1)
    else -> Color.Black
}

fun switchIcon(ctx: Context, alias: String) {
    val pm = ctx.packageManager
    listOf("IconBlue", "IconGreen", "IconPurple", "IconOrange").forEach { n ->
        pm.setComponentEnabledSetting(
            ComponentName(ctx, "com.unicom.monitor.$n"),
            if (n == alias) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

private val quotes = listOf(
    "人吃饭，说饭香，人洗碗，说碗脏。 ----网络",
    "猫是可爱的，狼是很帅的。就是说，孤独又可爱又帅。 ----我的青春恋爱物语果然有问题",
    "众口铄金，积毁销骨。 ----史记·张仪列传",
    "来啊，让我看看你们这些渣滓还有什么花样。 ----明日方舟",
    "博文天下，遍理人间！ ----自己",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: Settings,
    repo: SettingsRepository,
    onUpdate: (Settings) -> Unit,
    onPickImage: () -> Unit,
    onPickBg: () -> Unit,
    onPickAvatar: () -> Unit,
    onClose: () -> Unit,
    ctx: android.content.Context,
) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("设置") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = settings.cookie,
                    onValueChange = { v -> val s = settings.copy(cookie = v); onUpdate(s); scope.launch { repo.save(s) } },
                    label = { Text("cookie（必填）") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = settings.mobile,
                    onValueChange = { v -> val s = settings.copy(mobile = v); onUpdate(s); scope.launch { repo.save(s) } },
                    label = { Text("手机号") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("表情风格", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("classic" to "经典", "animal" to "动物", "color" to "色块", "tech" to "科技").forEach { (k, label) ->
                        FilterChip(
                            selected = settings.emojiSet == k,
                            onClick = { val s = settings.copy(emojiSet = k); onUpdate(s); scope.launch { repo.save(s) } },
                            label = { Text(label) }
                        )
                    }
                }
                Text("查询间隔", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 6, 12).forEach { h ->
                        FilterChip(
                            selected = settings.intervalHours == h,
                            onClick = {
                                val s = settings.copy(intervalHours = h); onUpdate(s); scope.launch { repo.save(s) }
                                val work = PeriodicWorkRequestBuilder<MonitorWorker>(h.toLong(), TimeUnit.HOURS).build()
                                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("unicom_monitor", ExistingPeriodicWorkPolicy.REPLACE, work)
                            },
                            label = { Text("${h}h") }
                        )
                    }
                }
                Text("字体颜色", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("black" to "黑", "white" to "白", "gray" to "深灰", "blue" to "深蓝").forEach { (k, label) ->
                        FilterChip(
                            selected = settings.textColor == k,
                            onClick = { val s = settings.copy(textColor = k); onUpdate(s); scope.launch { repo.save(s) } },
                            label = { Text(label) }
                        )
                    }
                }
                Text("查询间隔（分钟，最小 15）", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = settings.customInterval,
                    onValueChange = { v ->
                        val s = settings.copy(customInterval = v)
                        onUpdate(s); scope.launch { repo.save(s) }
                        val m = v.toIntOrNull()
                        if (m != null && m >= 15) {
                            val work = PeriodicWorkRequestBuilder<MonitorWorker>(m.toLong(), TimeUnit.MINUTES).build()
                            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("unicom_monitor", ExistingPeriodicWorkPolicy.REPLACE, work)
                        }
                    },
                    label = { Text("例如 30（分钟）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("表情包图片（通知）", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = onPickImage) { Text("选择") }
                    Button(onClick = { val s = settings.copy(imagePath = ""); onUpdate(s); scope.launch { repo.save(s) } }) { Text("清除") }
                }
                Text("聊天气泡背景", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = onPickBg) { Text("选择背景") }
                    Button(onClick = { val s = settings.copy(bgPath = ""); onUpdate(s); scope.launch { repo.save(s) } }) { Text("清除") }
                }
                Text("App 图标", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("IconBlue" to "蓝", "IconGreen" to "绿", "IconPurple" to "紫", "IconOrange" to "橙").forEach { (k, label) ->
                        Button(onClick = { switchIcon(ctx, k) }) { Text(label) }
                    }
                }
                Text("头像（聊天气泡左侧）", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = onPickAvatar) { Text("选择头像") }
                    Button(onClick = { val s = settings.copy(avatarPath = ""); onUpdate(s); scope.launch { repo.save(s) } }) { Text("清除") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("完成") } }
    )
}
