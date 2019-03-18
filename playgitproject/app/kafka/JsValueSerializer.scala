package kafka

import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.errors.SerializationException
import java.io.UnsupportedEncodingException
import java.util

import play.api.libs.json.JsValue

class JsValueSerializer extends Serializer[JsValue] {
  private val encoding = "UTF8"

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def serialize(topic: String, data: JsValue): Array[Byte] = {

    val optionalData: Option[JsValue] = Option(data)
    try {
      optionalData.map(_.toString.getBytes(encoding)).orNull
    } catch {
      case e: UnsupportedEncodingException =>
        throw new SerializationException("Error happened when trying to serialize JsValue to Byte Array")
    }
  }
    override def close(): Unit = {

    }

}