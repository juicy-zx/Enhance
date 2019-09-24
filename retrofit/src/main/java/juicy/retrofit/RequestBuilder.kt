package juicy.retrofit

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink

internal class RequestBuilder(
    headers: Headers?,
    isFormEncoded: Boolean,
    isMultipart: Boolean,
    private val method: String,
    private var relativeUrl: String,
    private var contentType: MediaType?,
    private val hasBody: Boolean
) {
    private val requestBuilder = Request.Builder()
    private val baseUrl: HttpUrl = relativeUrl.toHttpUrl()
    private var urlBuilder: HttpUrl.Builder? = null
    private var multipartBuilder: MultipartBody.Builder? = null
    private var formBuilder: FormBody.Builder? = null
    var body: RequestBody? = null

    init {
        headers?.also { requestBuilder.headers(it) }
        when {
            isFormEncoded -> formBuilder = FormBody.Builder()
            isMultipart -> {
                val builder = MultipartBody.Builder()
                builder.setType(MultipartBody.FORM)
                multipartBuilder = builder
            }
        }
    }

    internal fun addHeader(name: String, value: String) {
        if ("Content-Type".equals(name, ignoreCase = true)) {
            try {
                contentType = value.toMediaType()
            } catch (e: IllegalArgumentException) {
                throw  IllegalArgumentException("Malformed content type: $value", e)
            }
        } else {
            requestBuilder.addHeader(name, value)
        }
    }

    internal fun addQueryParam(name: String, value: String?, encoded: Boolean) {
        if (urlBuilder == null) {
            val url = relativeUrl
            // Do a one-time combination of the built relative URL and the base URL.
            urlBuilder = baseUrl.newBuilder(url)
        }
        if (encoded) {
            urlBuilder?.addEncodedQueryParameter(name, value)
        } else {
            urlBuilder?.addQueryParameter(name, value)
        }
    }

    internal fun addPart(headers: Headers, body: RequestBody) = multipartBuilder?.addPart(headers, body)

    internal fun addPart(part: MultipartBody.Part) = multipartBuilder?.addPart(part)

    internal fun addFormField(name: String, value: String, encoded: Boolean) {
        if (encoded) {
            formBuilder?.addEncoded(name, value)
        } else {
            formBuilder?.add(name, value)
        }
    }

    internal fun get(): Request.Builder {
        val urlBuilder = this.urlBuilder
        val url = if (urlBuilder != null) {
            urlBuilder.build()
        } else {
            // No query parameters triggered builder creation, just combine the relative URL and base URL.
            val relative = baseUrl.resolve(relativeUrl)
            requireNotNull(relative) { "Malformed URL. Base: $baseUrl, Relative: $relative" }
        }
        var body = this.body
        if (body == null) {
            // Try to pull from one of the builders.
            when {
                formBuilder != null -> body = formBuilder!!.build()
                multipartBuilder != null -> body = multipartBuilder!!.build()
                hasBody -> // Body is absent, make an empty body.
                    body = ByteArray(0).toRequestBody(null, 0, 0)
            }
        }
        val contentType = this.contentType
        if (contentType != null) {
            if (body != null) {
                body = ContentTypeOverridingRequestBody(body, contentType)
            } else {
                requestBuilder.addHeader("Content-Type", contentType.toString())
            }
        }
        return requestBuilder.url(url).method(method, body)
    }

    private class ContentTypeOverridingRequestBody(private val delegate: RequestBody, private val contentType: MediaType?) : RequestBody() {
        override fun contentType(): MediaType? = contentType
        override fun writeTo(sink: BufferedSink) = delegate.writeTo(sink)
        override fun contentLength(): Long = delegate.contentLength()
    }
}