package juicy.enhance

import okhttp3.Headers
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.IOException

internal sealed class ParameterHandler<T> {
    protected abstract val value: T?
    abstract fun apply(requestBuilder: RequestBuilder)
}

internal class Header<T>(private val name: String, override val value: T, private val converter: Converter<T, String>) :
        ParameterHandler<T>() {
    override fun apply(requestBuilder: RequestBuilder) {
        val headerValue = converter.convert(value) ?: return
        requestBuilder.addHeader(name, headerValue)
    }
}

internal class Query<T>(private val name: String, override val value: T?, private val encoded: Boolean, private val converter: Converter<T, String>?) :
        ParameterHandler<T>() {
    override fun apply(requestBuilder: RequestBuilder) {
        if (value == null) {
            requestBuilder.addQueryParam(name, null, encoded)
            return
        }
        val queryValue = converter?.convert(value)
        requestBuilder.addQueryParam(name, queryValue, encoded)
    }
}

internal class Field<T>(private val name: String, override val value: T, private val encoded: Boolean, private val converter: Converter<T, String>) :
        ParameterHandler<T>() {
    override fun apply(requestBuilder: RequestBuilder) {
        val formValue = converter.convert(value) ?: return
        requestBuilder.addFormField(name, formValue, encoded)
    }
}

internal class Part<T>(private val headers: Headers, override val value: T, private val converter: Converter<T, RequestBody>) :
        ParameterHandler<T>() {
    override fun apply(requestBuilder: RequestBuilder) {
        val body = try {
            converter.convert(value) ?: return
        } catch (e: IOException) {
            throw  RuntimeException("Unable to convert $value to RequestBody", e)
        }
        requestBuilder.addPart(headers, body)
    }
}

internal class RawPart(override val value: MultipartBody.Part) : ParameterHandler<MultipartBody.Part>() {
    override fun apply(requestBuilder: RequestBuilder) {
        requestBuilder.addPart(value)
    }
}

internal class Body<T : Any>(override val value: T, private val converter: Converter<T, RequestBody>) : ParameterHandler<T>() {
    override fun apply(requestBuilder: RequestBuilder) {
        val body = try {
            converter.convert(value) ?: return
        } catch (e: IOException) {
            throw  RuntimeException("Unable to convert $value to RequestBody", e)
        }
        requestBuilder.body = body
    }
}