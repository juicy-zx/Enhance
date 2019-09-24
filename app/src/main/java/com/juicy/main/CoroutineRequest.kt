package com.juicy.main

import juicy.retrofit.HttpMethod
import juicy.retrofit.IParamsBuilder
import juicy.retrofit.Retrofit
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.OkHttpClient
import kotlin.coroutines.CoroutineContext

inline fun <reified T> CoroutineScope.getAsync(
    context: CoroutineContext = Dispatchers.IO,
    callFactory: Call.Factory = HttpUtils.okHttpClient,
    crossinline builder: IParamsBuilder.() -> Unit
): Deferred<T?> {
    return async(context, CoroutineStart.LAZY) {
        val retrofit = Retrofit.create<T>(HttpMethod.GET)
        retrofit.paramsBuilder.apply(builder)
        retrofit.newCall(callFactory).execute()
    }
}

inline fun <reified T> CoroutineScope.postAsync(
    context: CoroutineContext = Dispatchers.IO,
    callFactory: Call.Factory = HttpUtils.okHttpClient,
    crossinline builder: IParamsBuilder.() -> Unit
): Deferred<T?> {
    return async(context, CoroutineStart.LAZY) {
        val retrofit = Retrofit.create<T>(HttpMethod.POST)
        retrofit.paramsBuilder.apply(builder)
        retrofit.newCall(callFactory).execute()
    }
}

inline fun <reified T> CoroutineScope.httpAsync(
    context: CoroutineContext = Dispatchers.IO,
    callFactory: Call.Factory = HttpUtils.okHttpClient,
    crossinline builder: IParamsBuilder.() -> Unit
): Deferred<T?> {
    return async(context, CoroutineStart.LAZY) {
        val retrofit = Retrofit.create<T>()
        retrofit.paramsBuilder.apply(builder)
        retrofit.newCall(callFactory).execute()
    }
}