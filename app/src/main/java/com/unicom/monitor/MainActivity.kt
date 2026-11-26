package com.unicom.monitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import java.io.File
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var balance by remember { mutableStateOf<BalanceData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { repo.settings.collect { settings = it } }

    val pickImage = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val file = File(ctx.filesDir, "emoji_image")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                settings = settings.copy(imagePath = file.absolutePath)
                scope.launch { repo.save(settings) }
                message = "表情包图片已保存"
            } catch (e: Exception) {
                message = "图片保存失败: ${e.message}"
            }
        }
    }

    fun refresh() {
        if (settings.cookie.isBlank()) { message = "请先填写 cookie"; return }
        loading = true; message = ""
        scope.launch {
            try {
                val d = UnicomApi.fetchBalance(settings.cookie, settings.mobile)
                balance = d
                NotificationHelper.notify(ctx, "联通余量", NotificationHelper.buildText(d, settings.emojiSet), settings.imagePath)
                message = "刷新成功"
            } catch (e: Exception) {
                message = "失败: ${e.message}"
            } finally { loading = false }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("联通流量监控", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("登录凭据", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = settings.cookie,
                    onValueChange = { v -> settings = settings.copy(cookie = v); scope.launch { repo.save(settings) } },
                    label = { Text("cookie") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = settings.mobile,
                    onValueChange = { v -> settings = settings.copy(mobile = v); scope.launch { repo.save(settings) } },
                    label = { Text("手机号") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("表情风格", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("classic" to "经典", "animal" to "动物", "color" to "色块", "tech" to "科技").forEach { (k, label) ->
                        FilterChip(
                            selected = settings.emojiSet == k,
                            onClick = { settings = settings.copy(emojiSet = k); scope.launch { repo.save(settings) } },
                            label = { Text(label) }
                        )
                    }
                }
                Text("预览: " + NotificationHelper.emojis(settings.emojiSet).joinToString(" "), fontSize = 13.sp)
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("表情包图片（可选）", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("选择图片") }
                    if (settings.imagePath.isNotEmpty()) {
                        Button(onClick = {
                            settings = settings.copy(imagePath = "")
                            scope.launch { repo.save(settings) }
                            message = "已清除图片"
                        }) { Text("清除") }
                    }
                }
                if (settings.imagePath.isNotEmpty()) {
                    val bmp = remember(settings.imagePath) { BitmapFactory.decodeFile(settings.imagePath) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "表情包预览",
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("查询间隔", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 6, 12).forEach { h ->
                        FilterChip(
                            selected = settings.intervalHours == h,
                            onClick = {
                                settings = settings.copy(intervalHours = h)
                                scope.launch { repo.save(settings) }
                                val work = PeriodicWorkRequestBuilder<MonitorWorker>(h.toLong(), TimeUnit.HOURS).build()
                                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                                    "unicom_monitor", ExistingPeriodicWorkPolicy.REPLACE, work
                                )
                                message = "已设置为每 $h 小时"
                            },
                            label = { Text("${h}h") }
                        )
                    }
                }
            }
        }

        Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
            Text(if (loading) "查询中..." else "立即查询并推送")
        }
        if (message.isNotEmpty()) Text(message, fontSize = 13.sp)

        balance?.let { d ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(d.packageName, fontWeight = FontWeight.Bold)
                    Text("总流量: ${"%.2f".format(d.totalGb)}G  剩余: ${"%.2f".format(d.remainGb)}G")
                    Text("已消耗: ${"%.2f".format(d.usedGb)}G  免流已用: ${"%.2f".format(d.freeUsedGb)}G")
                    Text("话费: ${d.fee}元  语音: ${d.voice}分  积分: ${d.point}")
                    Text("更新: ${d.updateTime}", fontSize = 12.sp)
                }
            }
        }
    }
}
