@file:Suppress("UNCHECKED_CAST")

package com.apurebase.kgraphql.schema.model

import com.apurebase.kgraphql.schema.Publisher
import com.apurebase.kgraphql.schema.SchemaException
import com.apurebase.kgraphql.schema.Subscriber
import com.apurebase.kgraphql.schema.structure.validateName
import com.fasterxml.jackson.databind.ObjectWriter
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect


private val subscribers = mutableMapOf<String, Subscriber?>()

/**
 * FunctionWrapper is common interface for classes storing functions registered in schema by server code.
 * Only up to 9 arguments are supported, because kotlin-reflect doesn't support
 * invoking lambdas, local and anonymous functions yet, making implementation severely limited.
 */
interface FunctionWrapper <T> : Publisher {
    //lots of boilerplate here, because kotlin-reflect doesn't support invoking lambdas, local and anonymous functions yet
    companion object {
        fun <T> on (function : KFunction<T>) : FunctionWrapper<T>
            = ArityN(function)

        fun <T> on (function : suspend () -> T) : FunctionWrapper<T>
            = ArityZero(function)

        fun <T, R> on (function : suspend (R) -> T)
            = ArityOne(function, false)

        fun <T, R> on (function : suspend (R) -> T, hasReceiver: Boolean = false)
            = ArityOne(function, hasReceiver)

        fun <T, R, E> on (function : suspend (R, E) -> T, hasReceiver: Boolean = false)
            = ArityTwo(function, hasReceiver)

        fun <T, R, E, W> on (function : suspend (R, E, W) -> T, hasReceiver: Boolean = false)
            = ArityThree(function, hasReceiver)

        fun <T, R, E, W, Q> on (function : suspend (R, E, W, Q) -> T, hasReceiver: Boolean = false)
            = ArityFour(function, hasReceiver)

        fun <T, R, E, W, Q, A> on (function : suspend (R, E, W, Q, A) -> T, hasReceiver: Boolean = false)
            = ArityFive(function, hasReceiver)

        fun <T, R, E, W, Q, A, S> on (function : suspend (R, E, W, Q, A, S) -> T, hasReceiver: Boolean = false)
            = AritySix(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G> on (function : suspend (R, E, W, Q, A, S, G) -> T, hasReceiver: Boolean = false)
            = AritySeven(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H> on (function : suspend (R, E, W, Q, A, S, G, H) -> T, hasReceiver: Boolean = false)
            = ArityEight(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J> on (function : suspend (R, E, W, Q, A, S, G, H, J) -> T, hasReceiver: Boolean = false)
            = ArityNine(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K> on (function : suspend (R, E, W, Q, A, S, G, H, J, K) -> T, hasReceiver: Boolean = false)
            = ArityTen(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L) -> T, hasReceiver: Boolean = false)
            = ArityEleven(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M) -> T, hasReceiver: Boolean = false)
            = ArityTwelve(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N) -> T, hasReceiver: Boolean = false)
            = ArityThirteen(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O) -> T, hasReceiver: Boolean = false)
            = ArityFourteen(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P) -> T, hasReceiver: Boolean = false)
            = ArityFifteen(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U) -> T, hasReceiver: Boolean = false)
            = AritySixteen(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V) -> T, hasReceiver: Boolean = false)
            = AritySeventeen(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X) -> T, hasReceiver: Boolean = false)
            = ArityEighteen(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y) -> T, hasReceiver: Boolean = false)
            = ArityNineteen(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0) -> T, hasReceiver: Boolean = false)
                = ArityTwenty(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1) -> T, hasReceiver: Boolean = false)
                = ArityTwentyOne(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2) -> T, hasReceiver: Boolean = false)
                = ArityTwentyTwo(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3) -> T, hasReceiver: Boolean = false)
                = ArityTwentyThree(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4) -> T, hasReceiver: Boolean = false)
                = ArityTwentyFour(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5) -> T, hasReceiver: Boolean = false)
                = ArityTwentyFive(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6) -> T, hasReceiver: Boolean = false)
                = ArityTwentySix(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6, R7> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6, R7) -> T, hasReceiver: Boolean = false)
                = ArityTwentySeven(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6, R7, R8> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6, R7, R8) -> T, hasReceiver: Boolean = false)
                = ArityTwentyEight(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9) -> T, hasReceiver: Boolean = false)
                = ArityTwentyNine(function, hasReceiver)

        fun <T, R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, E0> on (function : suspend (R, E, W, Q, A, S, G, H, J, K, L, M, N, O, P, U, V, X, Y, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, E0) -> T, hasReceiver: Boolean = false)
                = ArityThirty(function, hasReceiver)
    }

    val kFunction: KFunction<T>

    suspend fun invoke(vararg args: Any?) : T?
    suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter) : T?

    fun arity() : Int

    /**
     * denotes whether function is called with receiver argument.
     * Receiver argument in GraphQL is somewhat similar to kotlin receivers:
     * its value is passed by framework, usually it is parent of function property with [FunctionWrapper]
     * Receiver argument is omitted in schema, and cannot be stated in query document.
     */
    val hasReceiver : Boolean

    val argumentsDescriptor : Map<String, KType>

    abstract class Base<T> : FunctionWrapper<T>{
        private fun createArgumentsDescriptor(): Map<String, KType> {
            return valueParameters().associate { parameter ->
                val parameterName = parameter.name
                    ?: throw SchemaException("Cannot handle nameless argument on function: $kFunction")

                validateName(parameterName)
                parameterName to parameter.type
            }
        }

        override val argumentsDescriptor: Map<String, KType> by lazy { createArgumentsDescriptor() }

        override suspend fun invoke(vararg args: Any?) : T? {
            return invoke(*args)
        }
    }

    /**
     * returns list of function parameters without receiver
     */
    fun valueParameters(): List<kotlin.reflect.KParameter> {
        return kFunction.valueParameters.let {
            if(hasReceiver) it.drop(1) else it
        }
    }

    class ArityN<T>(override val kFunction: KFunction<T>): Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun arity() = kFunction.parameters.size

        override val hasReceiver: Boolean
            get() = kFunction.extensionReceiverParameter != null

        override suspend fun invoke(vararg args: Any?): T? {
            return kFunction.callSuspend(*args)
        }
    }

    class ArityZero<T>(val implementation : suspend ()-> T, override val hasReceiver: Boolean = false ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 0

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.isNotEmpty()) {
                val e = IllegalArgumentException("This function does not accept arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
            val t = implementation()
            subscribers.forEach{
                it.value?.onNext(t)
            }
            return t
        }
    }

    class ArityOne<T, R>(
        val implementation : suspend (R)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R)
                val subscription = args[0] as String
                subscribers[subscription]?.setArgs(subscriptionArgs.toTypedArray())
                subscribers[subscription]?.setObjectWriter(objectWriter)
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 1

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwo<T, R, E>(
        val implementation : suspend (R, E)-> T,
        override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }


        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 2

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityThree<T, R, E, W>(
        val implementation : suspend (R, E, W)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 3

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityFour<T, R, E, W, Q>(
        val implementation : suspend (R, E, W, Q)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 4

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityFive<T, R, E, W, Q, A>(
        val implementation : suspend (R, E, W, Q, A)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 5

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class AritySix<T, R, E, W, Q, A, S>(
        val implementation : suspend (R, E, W, Q, A, S)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 6

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class AritySeven<T, R, E, W, Q, A, S, D>(
        val implementation : suspend (R, E, W, Q, A, S, D)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 7

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityEight<T, R, E, W, Q, A, S, D, F>(
        val implementation : suspend (R, E, W, Q, A, S, D, F)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 8

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityNine<T, R, E, W, Q, A, S, D, F, G>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 9

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTen<T, R, E, W, Q, A, S, D, F, G, H>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 10

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityEleven<T, R, E, W, Q, A, S, D, F, G, H, I>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 11

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwelve<T, R, E, W, Q, A, S, D, F, G, H, I, J>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 12

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityThirteen<T, R, E, W, Q, A, S, D, F, G, H, I, J, K>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 13

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityFourteen<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 14

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityFifteen<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 15

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class AritySixteen<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 16

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class AritySeventeen<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 17

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityEighteen<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 18

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityNineteen<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 19

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwenty<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 20

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0)
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwentyOne<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 21

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1  )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }


    class ArityTwentyTwo<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 22

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2  )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }


    class ArityTwentyThree<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 23

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2, args[22] as R3  )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwentyFour<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 24

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2, args[22] as R3, args[23] as R4  )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwentyFive<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 25

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2, args[22] as R3, args[23] as R4, args[24] as R5  )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwentySix<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 26

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2, args[22] as R3, args[23] as R4, args[24] as R5, args[25] as R6 )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwentySeven<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6, R7>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6, R7)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 27

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2, args[22] as R3, args[23] as R4, args[24] as R5, args[25] as R6, args[26] as R7 )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwentyEight<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6, R7, R8>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6, R7, R8)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 28

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2, args[22] as R3, args[23] as R4, args[24] as R5, args[25] as R6, args[26] as R7, args[27] as R8 )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityTwentyNine<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 29

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2, args[22] as R3, args[23] as R4, args[24] as R5, args[25] as R6, args[26] as R7, args[27] as R8, args[28] as R9 )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    class ArityThirty<T, R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, E0>(
        val implementation : suspend (R, E, W, Q, A, S, D, F, G, H, I, J, K, L, M, N, O, P, U, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, E0)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {
        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>, objectWriter: ObjectWriter): T? {
            TODO("not needed")
        }
        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override val kFunction: KFunction<T>
            @Synchronized
            get() = implementation.reflect()!!

        override fun arity(): Int = 30

        override suspend fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G, args[9] as H, args[10] as I, args[11] as J, args[12] as K, args[13] as L, args[14] as M, args[15] as N, args[16] as O, args[17] as P, args[18] as U, args[19] as R0, args[20] as R1, args[21] as R2, args[22] as R3, args[23] as R4, args[24] as R5, args[25] as R6, args[26] as R7, args[27] as R8, args[28] as R9, args[29] as E0 )
                subscribers.forEach{
                    it.value?.onNext(t)
                }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach{
                    it.value?.onError(e)
                }
                throw e
            }
        }
    }

    fun hasReturnType(): Boolean = getObjectTypeName() != "java.lang.Void" && getObjectTypeName() != "kotlin.Unit"

    private fun getObjectTypeName() = kFunction.returnType.jvmErasure.javaObjectType.name
}
