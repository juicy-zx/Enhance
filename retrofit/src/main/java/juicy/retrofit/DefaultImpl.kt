package juicy.retrofit

import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.lang.reflect.Type

internal object DefaultConverterFactory : ConverterFactory() {
    override fun requestBodyConverter(type: Type): Converter<*, RequestBody>? =
        if (RequestBody::class.java.isAssignableFrom(type.getRawType())) RequestBodyConverter else null

    override fun responseBodyConverter(type: Type): Converter<ResponseBody, *>? =
        when (type) {
            ResponseBody::class.java -> ResponseBodyConverter
            String::class.java -> StringResponseBodyConverter
            Unit::class.java -> UnitResponseBodyConverter
            else -> null
        }

    override fun stringConverter(type: Type): Converter<*, String>? = StringConverter
}

private object RequestBodyConverter : Converter<RequestBody, RequestBody> {
    override fun convert(value: RequestBody): RequestBody = value
}

private object StringResponseBodyConverter : Converter<ResponseBody, String> {
    override fun convert(value: ResponseBody): String = value.string()
}

private object ResponseBodyConverter : Converter<ResponseBody, ResponseBody> {
    override fun convert(value: ResponseBody): ResponseBody = value
}

private object UnitResponseBodyConverter : Converter<ResponseBody, Unit> {
    override fun convert(value: ResponseBody) = Unit
}

private object StringConverter : Converter<Any, String> {
    override fun convert(value: Any): String = value.toString()
}

