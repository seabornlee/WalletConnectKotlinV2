package com.walletconnect.walletconnectv2.common.adapters

import com.squareup.moshi.*
import com.walletconnect.walletconnectv2.common.model.vo.TopicVO

object TopicAdapter: JsonAdapter<TopicVO>() {

    @FromJson
    @Qualifier
    override fun fromJson(reader: JsonReader): TopicVO? {
        reader.isLenient = true
        var topicValue: String? = null

        if (reader.hasNext() && reader.peek() == JsonReader.Token.STRING) {
            topicValue = reader.nextString()
        }

        return if (topicValue != null) {
            TopicVO(topicValue)
        } else {
            null
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, @Qualifier value: TopicVO?) {
        if (value != null) {
            writer.value(value.value)
        } else {
            writer.value("")
        }
    }

    @Retention(AnnotationRetention.RUNTIME)
    @JsonQualifier
    annotation class Qualifier
}