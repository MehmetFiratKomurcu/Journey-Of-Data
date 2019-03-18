package kafkaConsumePack

import java.time.Duration
import java.util.{Collections, Properties}

import kafka.utils.Logging
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import play.api.libs.json.JsValue


object kafkaConsumer extends App with Logging {

  val topic: String = "test"
  val brokers: String = "localhost:9092"
  val props = createConsumerConfig(brokers)
  val consumer = new KafkaConsumer[String, JsValue](props)
  run()

  def createConsumerConfig(brokers: String): Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "something")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "kafkaConsumePack.JsValueDeserializer")
    props
  }

  def run() = {
    consumer.subscribe(Collections.singletonList(this.topic))

    while(true) {
      val records = consumer.poll(Duration.ofMillis(100))
      records.forEach(x => print("record key: {} and record value: {} and record offset: {}", x.key, x.value, x.offset))
    }
  }

  def shutdown() = {
    if(consumer != null){
      consumer.close()
    }
  }

}
