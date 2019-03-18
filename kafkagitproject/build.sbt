name := "kafkagitproject"

version := "0.1"

scalaVersion := "2.12.8"

// https://mvnrepository.com/artifact/org.apache.kafka/kafka
libraryDependencies += "org.apache.kafka" %% "kafka" % "2.1.1"

// https://mvnrepository.com/artifact/com.typesafe.play/play
libraryDependencies += "com.typesafe.play" %% "play" % "2.7.0"

//libraryDependencies += "ch.qos.logback" % "logback-examples" % "1.3.0-alpha4"

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.8.0-beta4"