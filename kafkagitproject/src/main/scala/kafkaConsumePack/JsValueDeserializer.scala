package kafkaConsumePack

import java.io.UnsupportedEncodingException
import java.util

import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import play.api.libs.json.{JsValue, Json}

class JsValueDeserializer extends Deserializer[JsValue] {
  private val encoding = "UTF8"

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def deserialize(topic: String, data: Array[Byte]): JsValue = {
    val optionalData: Option[Array[Byte]] = Option(data)
    try{
      optionalData.map(new String(_, encoding)).map(Json.parse).orNull
    } catch {
      case e: UnsupportedEncodingException =>
        throw new SerializationException("error happened when trying to deserialize Array Byte to JsValue")
    }
  }

  override def close(): Unit = {

  }
}
