import SparkEngine.{schema, ufDF}
import SparkFormActor.UserFormDs
import SparkManager.SparkDataSet
import akka.actor.{Actor, ActorLogging, Props}
import org.apache.spark
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext._


import scala.util.Random
import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.sql.functions.from_json

object SparkManager {
  def props: Props = Props(new SparkManager)

  final case class SparkDataSet[T](ds: Dataset[T], dfType: String, ss: SparkSession)
}
class SparkManager extends Actor with ActorLogging {
  val sparkFormActor = context.actorOf(SparkFormActor.props, "SparkChildFormActor")
  context.watch(sparkFormActor)
  override def preStart(): Unit = {
    log.info("Spark Manager has Started!")
  }

  override def postStop(): Unit = {
    log.info("SparkManagerActor has Stopped!")
  }

  override def receive: Receive = {
    case SparkDataSet(ds, "UserForm", ss) => {
      sparkFormActor ! UserFormDs(ds, ss)
    }
  }
}

object SparkFormActor {
  def props: Props = Props(new SparkFormActor)

  final case class UserFormDs[T](ds: Dataset[T], ss: SparkSession)
}

class SparkFormActor extends Actor with ActorLogging {

  def ageStatus(age: Int): String = {
    age match {
      case age if (age > 40 &&  age <= 65) => "Old"
      case age if (age >= 25 && age <= 40) => "middle aged"
      case age if (age >= 18 && age <=25) => "Young"
      case _ => "age is not in range of 18-65"
    }
  }

  override def preStart(): Unit = {
    log.info("SparkFormActor has Started!")
  }

  override def postStop(): Unit = {
    log.info("SparkFormActor has Stopped!")
  }

  override def receive: Receive = {
    case UserFormDs(ds, ss) => {
      import ss.implicits._
      import org.apache.spark.sql.functions._
      val dsNew = ds.select($"value" cast "string" as "json")
        .select(from_json($"json", schema) as "data")
        .select("data.*")
      //printing schema of dsNew
      dsNew.printSchema()
      //drop rows that have missing data in every single row
      dsNew.na.drop("all")
      //registering as udf
      val ageStatusUDF = udf(ageStatus(_: Int): String)
      //using udf
      val dsWithAgeStatus = ds.select($"name", $"age", ageStatusUDF($"age").as("ageStatus"))
      println("---- " + ds.getClass)
    }
  }
}

object SparkEngine extends App{


  val schema = new StructType()
    .add("name", StringType)
    .add("password", StringType)
    .add("age", IntegerType)
    .add("email", StringType)
    .add("favSuperhero", StringType)
    .add("whyThisSuperhero", StringType)
    .add("address", new StructType()
      .add("country", StringType)
      .add("city", StringType)
      .add("street", StringType)
      .add("buildingNumber", IntegerType))

  val ss = SparkSession.builder().appName("sparkEngine").master("local[*]").getOrCreate()
  ss.sparkContext.setLogLevel("ERROR")
  import ss.implicits._
  import org.apache.spark.sql.functions._
  val ufDF = ss.readStream.format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9092")
    .option("subscribe", "test")
    .option("startingOffsets", """{"test": {"0":-2} }""")
    .load()
  println("--------" + ufDF.getClass)


  //writing to console
  val result = dsWithAgeStatus.writeStream.format("console").start()

  //val result = ufDF.writeStream.outputMode("complete").format("console").start()
  result.awaitTermination()


}
