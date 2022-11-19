package com.apurebase.kgraphql.schema.structure

import com.apurebase.kgraphql.schema.introspection.*
import kotlin.reflect.KClass

open class TypeProxy(var proxied: Type) : Type {

    override fun isInstance(value: Any?): Boolean = proxied.isInstance(value)

    override val kClass: KClass<*>?
        get() = proxied.kClass

    override val kind: TypeKind
        get() = proxied.kind

    override val name: String?
        get() = proxied.name

    override val description: String
        get() = proxied.description

    override val fields: List<__Field>?
        get() = proxied.fields

    override val interfaces: List<__Type>?
        get() = proxied.interfaces

    override val possibleTypes: List<__Type>?
        get() = proxied.possibleTypes

    override val enumValues: List<__EnumValue>?
        get() = proxied.enumValues

    override val inputFields: List<__InputValue>?
        get() = proxied.inputFields

    override val ofType: __Type?
        get() = proxied.ofType

    override fun get(name: String) = proxied[name]

}
