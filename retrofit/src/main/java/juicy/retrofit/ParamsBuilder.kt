package juicy.retrofit

import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import java.io.File

internal class ParamsBuilder : IParamsBuilder {
    private val parameterHandlers = ArrayList<ParameterHandler<*>>()
    private var contentType: MediaType? = null
    private var headers: Headers? = null
    override var hasBody: Boolean = false
    override var url: String? = null
    override var method: HttpMethod = HttpMethod.GET
    override var body: Any? = null
        set(value) {
            require(!(isFormEncoded || isMultipart)) { "Body parameters cannot be used with form or multi-part encoding." }
            if (value != null) {
                val type = value.javaClass
                val converter = Retrofit.requestBodyConverter<Any>(type)
                parameterHandlers.add(Body(value, converter))
            }
        }
    override var isFormEncoded: Boolean = false
        set(value) {
            require(!(value && isMultipart)) { "Only one encoding is allowed." }
            field = value
        }
    override var isMultipart: Boolean = false
        set(value) {
            require(!(value && isFormEncoded)) { "Only one encoding is allowed." }
            field = value
        }

    override fun query(vararg queries: Pair<String, Any?>, encoded: Boolean) = query(mapOf(*queries), encoded)
    override fun query(queries: Map<String, Any?>, encoded: Boolean) {
        for ((name, value) in queries) {
            if (value == null) {
                parameterHandlers.add(Query(name, value, encoded, null))
            } else {
                val type = value.javaClass
                val converter = Retrofit.stringConverter<Any>(type)
                parameterHandlers.add(Query(name, value, encoded, converter))
            }
        }
    }

    override fun header(vararg headers: Pair<String, Any>) = header(mapOf(*headers))
    override fun header(headers: Map<String, Any>) {
        for ((name, value) in headers) {
            val type = value.javaClass
            val converter = Retrofit.stringConverter<Any>(type)
            parameterHandlers.add(Header(name, value, converter))
        }
    }

    override fun headers(vararg headers: String) {
        val builder = Headers.Builder()
        for (header in headers) {
            val colon = header.indexOf(':')
            require(!(colon == -1 || colon == 0 || colon == header.length - 1)) {
                "Headers value must be in the form \"Name: Value\". Found: \"$header\"."
            }
            val headerName = header.substring(0, colon)
            val headerValue = header.substring(colon + 1).trim { it <= ' ' }
            if ("Content-Type".equals(headerName, ignoreCase = true)) {
                try {
                    contentType = headerValue.toMediaType()
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Malformed content type: $headerValue", e)
                }
            } else {
                builder.add(headerName, headerValue)
            }
        }
        this.headers = builder.build()
    }

    override fun field(vararg fields: Pair<String, Any>, encoded: Boolean) = field(mapOf(*fields), encoded)
    override fun field(fields: Map<String, Any>, encoded: Boolean) {
        for ((name, value) in fields) {
            val type = value.javaClass
            val converter = Retrofit.stringConverter<Any>(type)
            parameterHandlers.add(Field(name, value, encoded, converter))
        }
    }

    override fun part(vararg parts: Pair<String, Any>, encoding: String) = part(mapOf(*parts), encoding)
    override fun part(parts: Map<String, Any>, encoding: String) {
        for ((key, value) in parts) {
            val type = value.javaClass
            require(!MultipartBody.Part::class.java.isAssignableFrom(type.getRawType())) {
                "Part parameters using the MultipartBody.Part must not include a part name."
            }
            val converter = Retrofit.requestBodyConverter<Any>(type)
            val headers = Headers.headersOf(
                "Content-Disposition", "form-data; name=\"$key\"" +
                        if (value is File) "; filename=\"${value.name}\"" else "", "Content-Transfer-Encoding", encoding
            )
            parameterHandlers.add(Part(headers, value, converter))
        }
    }

    override fun part(vararg parts: MultipartBody.Part) = part(listOf(*parts))
    override fun part(parts: List<MultipartBody.Part>) {
        for (part in parts) {
            parameterHandlers.add(RawPart(part))
        }
    }

    internal fun create(): Request {
        val finalUrl = requireNotNull(url) { "url == null" }
        val handlers = parameterHandlers
        val requestBuilder = RequestBuilder(headers, isFormEncoded, isMultipart, method.method, finalUrl, contentType, hasBody)
        handlers.forEach { it.apply(requestBuilder) }
        return requestBuilder.get().build()
    }
}