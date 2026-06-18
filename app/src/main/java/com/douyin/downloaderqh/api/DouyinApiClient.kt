package com.douyin.downloaderqh.api

import com.douyin.downloaderqh.model.ApiResponse
import com.douyin.downloaderqh.model.XhsApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException

class DouyinApiClient {

    companion object {
        private const val DOUYIN_API = "https://api.bugpk.com/api/douyin"
        private const val XHS_API = "https://api.bugpk.com/api/xhs"

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun parseVideo(shareUrl: String): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("url", shareUrl)
                .build()

            val request = Request.Builder()
                .url(DOUYIN_API)
                .post(requestBody)
                .addHeader("User-Agent", "DouyinDownloader/2.0 (Android)")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: throw IOException("响应体为空")

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyString"))
            }

            val apiResponse = json.decodeFromString<ApiResponse>(bodyString)

            if (apiResponse.code == 200) Result.success(apiResponse)
            else Result.failure(IOException(apiResponse.msg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseXiaohongshu(shareUrl: String): Result<XhsApiResponse> = withContext(Dispatchers.IO) {
        try {
            val httpUrl = HttpUrl.Builder()
                .scheme("https")
                .host("api.bugpk.com")
                .addPathSegment("api")
                .addPathSegment("xhs")
                .addQueryParameter("url", shareUrl)
                .build()

            val request = Request.Builder()
                .url(httpUrl)
                .get()
                .addHeader("User-Agent", "DouyinDownloader/2.0 (Android)")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: throw IOException("响应体为空")

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyString"))
            }

            val xhsResponse = json.decodeFromString<XhsApiResponse>(bodyString)

            if (xhsResponse.code == 200) Result.success(xhsResponse)
            else Result.failure(IOException(xhsResponse.msg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
