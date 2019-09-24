package juicy.retrofit

import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.lang.reflect.Type

interface Converter<T, F> {
    fun convert(value: T): F?
}

abstract class ConverterFactory {
    open fun responseBodyConverter(type: Type): Converter<ResponseBody, *>? = null
    open fun requestBodyConverter(type: Type): Converter<*, RequestBody>? = null
    open fun stringConverter(type: Type): Converter<*, String>? = null
}

enum class HttpMethod(val method: String) {
    GET("GET"),
    POST("POST"),
    HEAD("HEAD"),
    PATCH("PATCH"),
    PUT("PUT"),
    OPTIONS("OPTIONS"),
    DELETE("DELETE");
}

interface IParamsBuilder {
    var method: HttpMethod
    var url: String?
    var isFormEncoded: Boolean
    var isMultipart: Boolean
    var hasBody: Boolean
    var body: Any?
    fun headers(vararg headers: String)
    fun query(vararg queries: Pair<String, Any?>, encoded: Boolean = false)
    fun query(queries: Map<String, Any?>, encoded: Boolean = false)
    fun header(vararg headers: Pair<String, Any>)
    fun header(headers: Map<String, Any>)
    fun field(vararg fields: Pair<String, Any>, encoded: Boolean = false)
    fun field(fields: Map<String, Any>, encoded: Boolean = false)
    fun part(vararg parts: MultipartBody.Part)
    fun part(parts: List<MultipartBody.Part>)
    fun part(vararg parts: Pair<String, Any>, encoding: String = "binary")
    fun part(parts: Map<String, Any>, encoding: String = "binary")
}

interface Call<T> : Cloneable {
    fun execute(): T?
    fun enqueue(onSuccess: (T?) -> Unit, onFailure: (Throwable) -> Unit)
    override fun clone(): Call<T>
    fun request(): Request
    fun isExecuted(): Boolean
    fun isCanceled(): Boolean
    fun cancel()
}