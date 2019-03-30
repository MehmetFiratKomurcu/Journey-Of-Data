import java.util.UUID

import SparkEngine.schema
import SparkFormActor.UserFormDs
import SparkManager.{DsFromChild, DsFromChildToCassandra, SparkDataSet}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.sql.functions.udf

object SparkManager {
  def props: Props = Props(new SparkManager)

  final case class SparkDataSet[T](ds: Dataset[T], dfType: HighData, ss: SparkSession)
  final case class DsFromChild[T](ds: Dataset[T])
  final case class DsFromChildToCassandra[T](ds: Dataset[T])
  trait HighData
  object UserFormData extends HighData
  object LogData extends HighData
}
class SparkManager extends Actor with ActorLogging {
  val sparkFormActor = context.actorOf(SparkFormActor.props, "SparkChildFormActor")
  context.watch(sparkFormActor)
  val cassandraActor = context.actorOf(CassandraActor.props, "CassandraChildActor")
  context.watch(cassandraActor)
  override def preStart(): Unit = {
    log.info("Spark Manager has Started!")
  }

  override def postStop(): Unit = {
    log.info("SparkManagerActor has Stopped!")
  }

  override def receive: Receive = {
    case SparkDataSet(ds, SparkManager.UserFormData, ss) => {
      sparkFormActor ! UserFormDs(ds, ss)
    }
    case DsFromChild(ds) => {
      sender() ! ds
    }
    case DsFromChildToCassandra(ds) => {
      cassandraActor ! CassandraActor.DatatoCs(ds, CassandraActor.UserDataType)
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
      case _ if age > 40 &&  age <= 65 => "Old"
      case _ if age >= 25 && age <= 40 => "middle aged"
      case _ if age >= 18 && age <=25 => "Young"
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
      //val dsWithAgeStatus = ds.select($"a", ageStatusUDF($"age").as("agestatus"))
      //val dsWithAgeStatus = dsNew.withColumn("agestatus", ageStatusUDF(col("age")))
      //generate uuid
      val generateUuid = udf(() => UUID.randomUUID().toString)
      val dsWithUuid = dsNew.withColumn("uuid", generateUuid())
      println("---- " + ds.getClass + " ----")
      dsWithUuid.printSchema()
      //val dsWithoutAddress = dsWithUuid.drop("address")
      //dsWithoutAddress.printSchema()
      sender() ! DsFromChildToCassandra(dsWithUuid)
      sender() ! DsFromChild(dsWithUuid)
    }
  }
}

object CassandraActor {
  def props: Props = Props(new CassandraActor)

  final case class DatatoCs[T](ds : Dataset[T], dataType: dataType)
  final case class UserData(id: UUID, firstname: String, lastname: String, age: Int, favSuperhero: String,
                            whyThisSuperhero: String, Address: String)
  final case class LogtoCs[T](ds: Dataset[T], dataType: dataType)
  trait dataType
  object UserDataType extends dataType
  object LogDataType extends dataType
}

class CassandraActor extends Actor with ActorLogging {
  import CassandraActor._
  override def preStart(): Unit = {
    log.info("cassandra actor has started")
  }

  override def postStop(): Unit = {
    log.info("cassandra actor has stopped")
  }

  override def receive: Receive = {
    case DatatoCs(ds, UserDataType) => {
      /*
      val query = ds.writeStream.format("org.apache.spark.sql.cassandra")
                    .option("keyspace", "projectjod").option("table", "user")
                    .start()
                    */
      import org.apache.spark.sql.cassandra._

      ds.writeStream.foreachBatch{(batchDF, _) =>
        batchDF.write.cassandraFormat("user", "projectjod")
          .mode("append")
          .save()
      }.start()

    }
    case LogtoCs(ds, CassandraActor.LogDataType) => {
      import org.apache.spark.sql.cassandra._
      val generateUuid = udf(() => UUID.randomUUID().toString)
      val dsWithUuid = ds.withColumn("uuid", generateUuid())


      dsWithUuid.writeStream.foreachBatch{(batchDF, _) =>
        batchDF.write.cassandraFormat("logs", "projectjod")
          .mode("append")
          .save()
      }.start()


    }
  }
}

object SparkEngine extends App{


  val schema = new StructType()
    .add("name", StringType)
    .add("password", StringType)
    .add("age", IntegerType)
    .add("email", StringType)
    .add("favsuperhero", StringType)
    .add("whythissuperhero", StringType)
    .add("address", new StructType()
      .add("country", StringType)
      .add("city", StringType)
      .add("street", StringType)
      .add("buildingnumber", IntegerType))

  val ss = SparkSession.builder().appName("sparkEngine").master("local[*]")
          .config("spark.cassandra.connection.host", "127.0.0.1").getOrCreate()
  ss.sparkContext.setLogLevel("ERROR")

  import ss.implicits._
  import org.apache.spark.sql.functions._

  val ufDF = ss.readStream.format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9092")
    .option("subscribe", "test")
    .option("startingOffsets", """{"test": {"0":-2} }""")
    .load()

  val logDf = ss.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "logs")
      .load()
      .selectExpr("CAST(value AS STRING)")
  logDf.printSchema()
  println("--------" + ufDF.getClass)

  val system = ActorSystem("ActorSystem")
  val sManagerActor = system.actorOf(SparkManager.props, "Spark-Manager-Actor")
  val cassandraActor = system.actorOf(CassandraActor.props, "Cassandra-Actor")
  cassandraActor ! CassandraActor.LogtoCs(logDf, CassandraActor.LogDataType)
  import scala.concurrent.duration._
  import akka.util.Timeout
  implicit val timeout = Timeout(5 seconds)
  import akka.pattern.ask
  val future = sManagerActor ? SparkDataSet(ufDF, SparkManager.UserFormData, ss)
  import scala.concurrent.Await
  //val ans = Await.result(future, timeout.duration).asInstanceOf[Dataset[String]]
  //writing to console
  import scala.concurrent.ExecutionContext.Implicits.global
  future.onSuccess{
    case ans: Dataset[String] =>
      val result = ans.writeStream.format("console").start()
      result.awaitTermination()
  }
  //val result = ans.writeStream.format("console").start()

  //val result = ufDF.writeStream.outputMode("complete").format("console").start()
  //result.awaitTermination()


}
