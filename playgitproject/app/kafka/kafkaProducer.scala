package kafka

import java.util.{Date, Properties}

import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.Random


import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import play.api.libs.json.JsValue
import scala.util.Random

class kafkaProducer(topic: String, brokers: String, msg: JsValue) {

  val rnd = new Random()
  val props = new Properties()

  props.put("bootstrap.servers", brokers)
  //props.put("client.id", "kafkaProducer")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer","kafka.JsValueSerializer")
  val producer = new KafkaProducer[String, JsValue](props)

  def run() = {

    val t = System.currentTimeMillis()
    for(_ <- Range(0, 10)) {
      //val ip = "192.168.2." + rnd.nextInt(255)
      val data = new ProducerRecord[String, JsValue](topic, "myKey",msg)
      producer.send(data)
    }
    println("sent per second : " + 10 * 1000 / (System.currentTimeMillis - t))
  }

  def shutdown() = {
    if(producer != null)
      producer.close()
  }

}

