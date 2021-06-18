package com.juicy.main

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import juicy.enhance.Enhance
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

object HttpUtils {
    private const val DEFAULT_TIMEOUT: Long = 45
    val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        builder.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        builder.readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        val httpCacheDirectory = File(App.instance.cacheDir, "OkHttpCache")
        builder.cache(Cache(httpCacheDirectory, (10 * 1024 * 1024).toLong()))
        builder.addInterceptor(CacheControlInterceptor())
        //Interceptor
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            loggingInterceptor.redactHeader("Authorization")
            loggingInterceptor.redactHeader("Cookie")
            builder.addNetworkInterceptor(loggingInterceptor)
        }
        builder.build()
    }

    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Int::class.java, IntegerDefaultAdapter())
            .registerTypeAdapter(Double::class.java, DoubleDefaultAdapter())
            .registerTypeAdapter(Long::class.java, LongDefaultAdapter())
            .registerTypeAdapter(Boolean::class.java, BooleanDefaultAdapter())
            .registerTypeAdapter(Float::class.java, FloatDefaultAdapter())
            .registerTypeAdapter(String::class.java, StringDefaultAdapter())
            .create()
    }

    val enhance: Enhance by lazy {
        Enhance.Builder().apply {
            callFactory = okHttpClient
            addConverterFactory(GsonConverterFactory.create(gson))
        }.build()
    }

    private class CacheControlInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            if (!checkConnectivity()) {
                request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build()
            }
            val response = chain.proceed(request)
            if (checkConnectivity()) {
                val maxAge = 60 * 60
                response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, max-age=$maxAge")
                    .build()
            } else {
                val maxStale = 60 * 60 * 24 * 28
                response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                    .build()
            }
            return response
        }

        fun checkConnectivity(): Boolean {
            val connectivityManager = App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = connectivityManager.activeNetworkInfo
            return info != null && info.isAvailable
        }
    }
}