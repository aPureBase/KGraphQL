package com.apurebase.kgraphql

import com.apurebase.kgraphql.helpers.toJsonElement
import com.apurebase.kgraphql.schema.model.ast.ASTNode
import com.apurebase.kgraphql.schema.model.ast.Location.Companion.getLocation
import com.apurebase.kgraphql.schema.model.ast.Source
import kotlinx.serialization.json.*

open class GraphQLError(

    /**
     * A message describing the Error for debugging purposes.
     */
    message: String,

    /**
     * An array of GraphQL AST Nodes corresponding to this error.
     */
    val nodes: List<ASTNode>? = null,

    /**
     * The source GraphQL document for the first location of this error.
     *
     * Note that if this Error represents more than one node, the source may not
     * represent nodes after the first node.
     */
    val source: Source? = null,

    /**
     * An array of character offsets within the source GraphQL document
     * which correspond to this error.
     */
    val positions: List<Int>? = null,

    /**
     * The original error thrown from a field resolver during execution.
     */
    val originalError: Throwable? = null,

    /**
     * Change 1: Added extensions to the error response.
     */
    val extensionsErrorType: String? = "INTERNAL_SERVER_ERROR",
    val extensionsErrorDetail: Map<String, Any?>? = null
) : Exception(message) {

    constructor(message: String, node: ASTNode?) : this(message, nodes = node?.let(::listOf))
    constructor(message: String, extensionsErrorType: String?,extensionsErrorDetail:Map<String, Any?>?) : this(message,null,null,null,null,extensionsErrorType,extensionsErrorDetail )
    constructor(message: String, extensionsErrorType: String?) : this(message,null,null,null,null,extensionsErrorType )


    /**
     * An array of { line, column } locations within the source GraphQL document
     * that correspond to this error.
     *
     * Errors during validation often contain multiple locations, for example to
     * point out two things with the same name. Errors during execution include a
     * single location, the field that produced the error.
     */
    val locations: List<Source.LocationSource>? by lazy {
            if (positions != null && source != null) {
            positions.map { pos -> getLocation(source, pos) }
        } else nodes?.mapNotNull { node ->
            node.loc?.let { getLocation(it.source, it.start) }
        }
    }

    fun prettyPrint(): String {
        var output = message ?: ""

        if (nodes != null) {
            for (node in nodes) {
                if (node.loc != null) {
                    output += "\n\n" + node.loc!!.printLocation()
                }
            }
        } else if (source != null && locations != null) {
            for (location in locations!!) {
                output += "\n\n" + source.print(location)
            }
        }

        return output
    }

    /**
     * Change 1: Added extensions to the error response.
     * Change 2: Added debug option to GraphQL Configuration (flag to output exception information to extensions)
     * Change 3: Moved serialize (), which was defined as an extension of GraphQLError, into GraphQLError. option
     */
    open val extensions: Map<String,Any?>?by lazy {
        val extensions = mutableMapOf<String,Any?>()
        extensionsErrorType?.let{  extensions.put("type",extensionsErrorType) }
        extensionsErrorDetail?.let { extensions.put("detail",extensionsErrorDetail) }
        extensions
    }

    open fun extensionsDebug(): Map<String,Any?> {
        val extensions = mutableMapOf<String,Any?>()
        extensionsErrorType?.let{  extensions.put("type",extensionsErrorType) }
        extensionsErrorDetail?.let { extensions.put("detail",extensionsErrorDetail) }
        extensions.put("debug",this.debugInfo())
        return extensions
    }

    open fun debugInfo(): Map<String,Any?> {
        val exception = mutableMapOf<String,Any?>()
        val stackList = this.stackTrace
        if (!stackList[0].fileName.isNullOrEmpty()) {
            exception.put("fileName",stackList[0].fileName)
            exception.put("line",stackList[0].lineNumber.toString())
        }
        if (!stackList[0].methodName.isNullOrEmpty()) {
            exception.put("method",stackList[0].methodName)
        }
        if (!stackList[0].className.isNullOrEmpty()) {
            exception.put("classPath", stackList[0].className)
        }
        exception.put("stackTrace", stackList)
        return  exception
    }

    open fun serialize(debug:Boolean=false): String = buildJsonObject {
        put("errors", buildJsonArray {
            addJsonObject {
                put("message", message)
                put("locations", buildJsonArray {
                    locations?.forEach {
                        addJsonObject {
                            put("liane", it.line)
                            put("column", it.column)
                        }
                    }
                })
                put("path", buildJsonArray {
                    // TODO: Build this path. https://spec.graphql.org/June2018/#example-90475
                })
                if (!debug) {
                    extensions?.let {
                        put("extensions", it.toJsonElement())
                    }
                } else {
                    extensionsDebug().let {
                        put("extensions", it.toJsonElement())
                    }
                }
            }
        })
    }.toString()
}
