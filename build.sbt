name := "nfer"

version := "1.1"

scalaVersion := "2.13.5"

//libraryDependencies ++= Seq(
//  // https://akka.io/docs/
//  "com.typesafe.akka" %% "akka-actor" % "2.5.32",
//  // https://mvnrepository.com/artifact/org.json4s/json4s-jackson
//  "org.json4s" %% "json4s-jackson" % "3.6.11",
//  // https://search.maven.org/search?q=a:kafka_2.13
//  "org.apache.kafka" %% "kafka" % "2.4.1",
//  // https://search.maven.org/artifact/org.fusesource.stomp/scomp
//  "org.fusesource.stomp" % "scomp" % "1.0.0",
//  // https://github.com/scala/scala-parser-combinators
//  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
//  "com.lihaoyi" %% "ujson" % "1.3.12"
//)

libraryDependencies ++= Seq(
  // https://akka.io/docs/
  "com.typesafe.akka" %% "akka-actor" % "2.6.14",
  // https://mvnrepository.com/artifact/org.json4s/json4s-jackson
  "org.json4s" %% "json4s-jackson" % "3.6.11",
  // https://search.maven.org/search?q=a:kafka_2.13
  "org.apache.kafka" %% "kafka" % "2.4.1",
  // https://search.maven.org/artifact/org.fusesource.stomp/scomp
  "org.fusesource.stomp" % "scomp" % "1.0.0",
  // https://github.com/scala/scala-parser-combinators
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "com.lihaoyi" %% "ujson" % "1.3.12"
)

// unmanagedBase := baseDirectory.value / "src/main/resources"


