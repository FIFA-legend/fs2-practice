package com.itechart

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.itechart.util.MongoClientImpl
import com.itechart.util.imports._
import fs2.Stream
import neotypes.cats.effect.implicits._
import neotypes.fs2.Fs2IoStream
import neotypes.fs2.implicits._
import neotypes.implicits.syntax.cypher._
import neotypes.{GraphDatabase, StreamingDriver}
import org.bson.Document
import org.neo4j.driver.AuthTokens

object Main extends App {

  val mongoHost = "localhost"
  val mongoPort = 27017
  val mongoUser = "root"
  val mongoPassword = "0987654321KnKn"

  val neo4jHost = "localhost"
  val neo4jPort = 7687
  val neo4jUser = "neo4j"
  val neo4jPassword = "streams"

  final case class Car(brand: String, model: String, year: Int)
  final case class Person(name: String, surname: String, car: Car)

  val mongoUrl = s"mongodb://$mongoUser:$mongoPassword@$mongoHost:$mongoPort"
  val allPersons: Stream[IO, Person] =
    for {
      conn <- Stream.resource(MongoClientImpl.fromUrl[IO](mongoUrl))
      database = conn.getDatabase("fs2practice")
      collection = database.getCollection("persons")
      document <- collection.find().stream[IO]
      name = document.get("name", classOf[String])
      surname = document.get("surname", classOf[String])
      carDocument = document.get("car", classOf[Document])
      brand = carDocument.get("brand", classOf[String])
      model = carDocument.get("model", classOf[String])
      year = carDocument.get("year", classOf[Integer])
    } yield Person(name, surname, Car(brand, model, year))

  allPersons.evalMap(d => IO(println(d))).compile.drain.unsafeRunSync()

  /*val persons: Stream[IO, Person] = Stream(
    Person("Nikita", "Kolodko", Car("Porsche", "Panamera", 2020)),
    Person("Default", "Person", Car("Audi", "A4", 2019))
  ).covary[IO]*/

  val neo4jDriver: Resource[IO, StreamingDriver[Fs2IoStream, IO]] =
    GraphDatabase.streamingDriver[Fs2IoStream](s"bolt://$neo4jHost:$neo4jPort", AuthTokens.basic(neo4jUser, neo4jPassword))

  val program: Stream[IO, Unit] = for {
    dr <- Stream.resource(neo4jDriver)
    person <- allPersons
    _ <- c"CREATE (p: Person { name: ${person.name}, surname: ${person.surname} })-[:OWNS]->(c: Car { brand: ${person.car.brand}, model: ${person.car.model}, year: ${person.car.year} })"
      .query[Unit].stream(dr)
  } yield ()

  program.compile.drain.unsafeRunSync()
}
