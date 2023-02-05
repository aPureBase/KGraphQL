package com.apurebase.kgraphql.schema.dsl.operations

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.schema.dsl.LimitedAccessItemDSL
import com.apurebase.kgraphql.schema.dsl.ResolverDSL
import com.apurebase.kgraphql.schema.model.FunctionWrapper
import com.apurebase.kgraphql.schema.model.InputValueDef
import kotlin.reflect.KFunction
import kotlin.reflect.KType


abstract class AbstractOperationDSL(
    val name: String
) : LimitedAccessItemDSL<Nothing>(),
    ResolverDSL.Target {

    protected val inputValues = mutableListOf<InputValueDef<*>>()

    internal var functionWrapper: FunctionWrapper<*>? = null

    var explicitReturnType: KType? = null

    private fun resolver(function: FunctionWrapper<*>): ResolverDSL {

        try {
            require(function.hasReturnType()) {
                "Resolver for '$name' has no return value"
            }
        } catch (e: Throwable) {
            if ("KotlinReflectionInternalError" !in e.toString()) {
                throw e
            }
        }

        functionWrapper = function
        return ResolverDSL(this)
    }

    fun <T> KFunction<T>.toResolver() = resolver(FunctionWrapper.on(this))

    fun <T> resolver(function: suspend () -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R> resolver(function: suspend (R) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E> resolver(function: suspend (R, E) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W> resolver(function: suspend (R, E ,W ) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q> resolver(function: suspend (R, E, W, Q) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A> resolver(function: suspend (R, E, W, Q, A) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S> resolver(function: suspend (R, E, W, Q, A, S) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B> resolver(function: suspend (R, E, W, Q, A, S, B) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U> resolver(function: suspend (R, E, W, Q, A, S, B, U) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C> resolver(function: suspend (R, E, W, Q, A, S, B, U, C) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2) -> T) = resolver(FunctionWrapper.on(function))

    @JvmName("resolver1")
    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2, R3> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2, R3) -> T) = resolver(FunctionWrapper.on(function))

    @JvmName("resolver2")
    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2, R3, R4> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2, R3, R4) -> T) = resolver(FunctionWrapper.on(function))

    @JvmName("resolver3")
    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2, R3, R4, R5> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2, R3, R4, R5) -> T) = resolver(FunctionWrapper.on(function))


//    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, G1, G2, R2, R3> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, G1, G2, R2, R3) -> T) = resolver(FunctionWrapper.on(function))


//    fun <T, R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2, R3, R4, R5, R6> resolver(function: suspend (R, E, W, Q, A, S, B, U, C, D, F, G, H, I, J, K, L, M, N, R0, R1, R2, R3, R4, R5, R6) -> T) = resolver(FunctionWrapper.on(function))


    fun accessRule(rule: (Context) -> Exception?){
        val accessRuleAdapter: (Nothing?, Context) -> Exception? = { _, ctx -> rule(ctx) }
        this.accessRuleBlock = accessRuleAdapter
    }

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }

    override fun setReturnType(type: KType) {
        explicitReturnType = type
    }

}
