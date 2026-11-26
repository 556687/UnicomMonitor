package com.unicom.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class BalanceData(
    val packageName: String = "",
    val totalGb: Double = 0.0,
    val usedGb: Double = 0.0,
    val freeUsedGb: Double = 0.0,
    val remainGb: Double = 0.0,
    val dailyMb: String = "0",
    val fee: String = "",
    val voice: String = "",
    val point: String = "",
    val updateTime: String = "",
)

object UnicomApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val UA = "Mozilla/5.0 (Linux; Android 17; 22127RK46C Build/CP2A.260605.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/157.0.8103.50 Mobile Safari/537.36; unicom{version:android@13.0201,desmobile:0};devicetype{deviceBrand:Xiaomi,deviceModel:22127RK46C};OSVersion/17;ltst;"

    suspend fun fetchBalance(cookie: String, mobile: String): BalanceData = withContext(Dispatchers.IO) {
        val flowUrl = "https://mxx.client.10010.com/servicequerybusiness/operationservice/queryOcsPackageFlowLeftContentRevisedInJune"
        val body = "duanlianjieabc=&channelCode=&serviceType=&saleChannel=&externalSources=&contactCode=&ticket=&ticketPhone=&ticketChannel=&mobile1=&language=chinese"
        val flowReq = Request.Builder().url(flowUrl).post(
            okhttp3.RequestBody.create(null, body)
        ).header("Cookie", cookie).header("User-Agent", UA)
            .header("Content-Type", "application/x-www-form-urlencoded").build()
        val flowJson = JSONObject(client.newCall(flowReq).execute().use { it.body!!.string() })

        var total = 0.0; var used = 0.0; var freeUsed = 0.0
        val details = flowJson.optJSONObject("shareData")?.optJSONArray("details")
        if (details != null) {
            for (i in 0 until details.length()) {
                val d = details.getJSONObject(i)
                total += d.optString("total").toDoubleOrNull() ?: 0.0
                used += d.optString("use").toDoubleOrNull() ?: 0.0
                val name = d.optString("addUpItemName")
                if (name.contains("免费") || name.contains("免流")) {
                    freeUsed += d.optString("use").toDoubleOrNull() ?: 0.0
                }
            }
        }
        var remain = 0.0
        val sums = flowJson.optJSONArray("flowSumList")
        if (sums != null) {
            for (i in 0 until sums.length()) {
                remain += sums.getJSONObject(i).optString("xcanusevalue").toDoubleOrNull() ?: 0.0
            }
        }
        val daily = flowJson.optJSONObject("summary")?.optString("domesticDailyFlow") ?: "0"

        val userUrl = "https://m.client.10010.com/mobileserviceimportant/home/queryUserInfoSeven?currentPhone=$mobile&showType=0&version=android@13.0201&desmobile=$mobile"
        val userReq = Request.Builder().url(userUrl).header("Cookie", cookie).header("User-Agent", UA).build()
        val userJson = JSONObject(client.newCall(userReq).execute().use { it.body!!.string() })

        var fee = ""; var voice = ""; var point = ""
        val list = userJson.optJSONObject("data")?.optJSONArray("dataList")
        if (list != null) {
            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                when (item.optString("type")) {
                    "fee" -> fee = item.optString("number")
                    "voice" -> voice = item.optString("number")
                    "point" -> point = item.optString("number")
                }
            }
        }
        BalanceData(
            packageName = flowJson.optString("packageName"),
            totalGb = total / 1024.0,
            usedGb = ((used - freeUsed).coerceAtLeast(0.0)) / 1024.0,
            freeUsedGb = freeUsed / 1024.0,
            remainGb = remain / 1024.0,
            dailyMb = daily,
            fee = fee, voice = voice, point = point,
            updateTime = flowJson.optString("time"),
        )
    }
}
