package juicy.retrofit

import java.lang.reflect.*
import java.lang.reflect.Array

internal fun Type?.getRawType(): Class<*> {
    require(this != null) { throw NullPointerException("type == null") }
    if (this is Class<*>) {
        return this
    }
    if (this is ParameterizedType) {
        val rawType = rawType
        require(rawType is Class<*>) { throw IllegalArgumentException() }
        return rawType
    }
    if (this is GenericArrayType) {
        val componentType = genericComponentType
        return Array.newInstance(componentType.getRawType(), 0)::class.java
    }
    if (this is TypeVariable<*>) {
        return Any::class.java
    }
    if (this is WildcardType) {
        return upperBounds[0].getRawType()
    }
    throw IllegalArgumentException(
        "Expected a Class, ParameterizedType, or" +
                "GenericArrayType, but <$this> is of type ${this.javaClass.name}"
    )
}

internal fun throwIfFatal(t: Throwable) {
    when (t) {
        is VirtualMachineError -> throw t
        is ThreadDeath -> throw t
        is LinkageError -> throw t
    }
}

open class TypeBase<E>