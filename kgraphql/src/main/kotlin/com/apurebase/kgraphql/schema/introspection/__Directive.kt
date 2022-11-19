package com.apurebase.kgraphql.schema.introspection

import com.apurebase.kgraphql.schema.directive.DirectiveLocation


interface __Directive : __Described {

    val locations: List<DirectiveLocation>

    val args: List<__InputValue>
}