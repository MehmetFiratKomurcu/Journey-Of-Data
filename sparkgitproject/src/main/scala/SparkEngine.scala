import org.apache.spark
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext._

import scala.util.Random
import org.apache.spark.sql.types._
import org.apache.spark.sql._

case class Address(country: String, city: String, street: String, buildingNumber: Int)
case class UserForm(name: String, password: String, age: Int, email: String, favSuperhero: String,
                    whyThisSuperhero: Option[String], address: Address)

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

  val ds = ufDF.select($"value" cast "string" as "json")
    .select(from_json($"json", schema) as "data")
    .select("data.*", "data.address.*")
  /*
  val ds = ufDF.select($"value" cast "string" as "json")
      .select(from_json($"json", schema) as "data")
      .select("data.name", "data.age", "data.email", "data.favSuperhero", "data.whyThisSuperhero",
      "data.address.country", "data.address.city", "data.address.street", "data.address.buildingNumber")*/

  //val dasda = ufDF.selectExpr("CAST(value AS STRING)")
  //println("----------- " + dasda)
  //ufDF.printSchema()

  ds.printSchema()
  val result = ds.writeStream.format("console").start()

  //val result = ufDF.writeStream.outputMode("complete").format("console").start()
  result.awaitTermination()


}
