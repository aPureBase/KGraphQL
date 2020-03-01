package com.apurebase.kgraphql

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.*


@Serializable
data class GraphqlRequest(
        val query: String,
        val variables: JsonObject?//,
        //val operationName: String?
) {

    @Serializer(forClass = GraphqlRequest::class)
    companion object : KSerializer<GraphqlRequest> {
        override val descriptor: SerialDescriptor = object : SerialClassDescImpl("GraphqlRequest") {
            init {
                addElement("query")
                addElement("variables")
                //addElement("operationName")
            }
        }

        override fun serialize(encoder: Encoder, obj: GraphqlRequest) {
            /*val compositeOutput = encoder.beginStructure(descriptor)
            compositeOutput.encodeStringElement(descriptor, 0, obj.finalQuery)
            compositeOutput.encodeStringElement(descriptor, 1, obj.variables)
            compositeOutput.endStructure(descriptor)*/
            throw UpdateNotSupportedException("Serialization not supported")
        }

        private val deserializationStrategy = object : DeserializationStrategy<JsonObject?> {
            override val descriptor: SerialDescriptor = SerialClassDescImpl("JsonObject")

            override fun deserialize(decoder: Decoder): JsonObject? {
                val input = decoder as? JsonInput ?: throw SerializationException("Expected Json Input")
                val jsonObject = input.decodeJson()

                return if (jsonObject is JsonObject) jsonObject else null
            }

            override fun patch(decoder: Decoder, old: JsonObject?): JsonObject? {
                throw UpdateNotSupportedException("Update not supported")
            }

        }

        override fun deserialize(decoder: Decoder): GraphqlRequest {
            val dec: CompositeDecoder = decoder.beginStructure(descriptor)
            var query = ""
            var variables: JsonObject? = null
            //var operationName: String? = null
            loop@ while (true) {
                when (val i = dec.decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_DONE -> break@loop
                    0 -> query = dec.decodeStringElement(descriptor, i)
                    1 -> variables = dec.decodeNullableSerializableElement(descriptor, i, deserializationStrategy)
                    //2 -> operationName = dec.decodeStringElement(descriptor, i)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            dec.endStructure(descriptor)
            return GraphqlRequest(
                    query,
                    variables//,
                    //operationName
            )
        }
    }
}