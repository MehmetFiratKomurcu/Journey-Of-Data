name := "playgitproject"
 
version := "1.0" 
      
lazy val `playgitproject` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

// https://mvnrepository.com/artifact/org.apache.kafka/kafka
libraryDependencies += "org.apache.kafka" %% "kafka" % "2.1.1"

// https://mvnrepository.com/artifact/com.twitter/chill
libraryDependencies += "com.twitter" %% "chill" % "0.9.2"      

//for send logs to kafka
// https://mvnrepository.com/artifact/com.github.danielwegener/logback-kafka-appender
libraryDependencies += "com.github.danielwegener" % "logback-kafka-appender" % "0.2.0-RC2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"