name := "fs2-practice"

version := "0.1"

scalaVersion := "2.13.8"

val fs2Version = "3.2.7"
val neo4jVersion = "0.20.0"

libraryDependencies ++= Seq(
  // fs2
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-reactive-streams" % fs2Version,

  // mongo
  "org.mongodb" % "mongodb-driver-async" % "3.8.0",

  // neo4j
  "org.neo4j.driver" % "neo4j-java-driver" % "4.4.6",
  "io.github.neotypes" %% "neotypes-core" % neo4jVersion,
  "io.github.neotypes" %% "neotypes-cats-effect" % neo4jVersion,
  "io.github.neotypes" %% "neotypes-fs2-stream" % neo4jVersion
)
