package com.itechart

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.itechart.Main.{Car, Person}
import com.itechart.util.MongoClientImpl
import com.mongodb.async.SingleResultCallback
import fs2.Stream
import org.bson.Document

import scala.util.Random

object LoadMongo extends App {

  val mongoHost = "localhost"
  val mongoPort = 27017
  val mongoUser = "root"
  val mongoPassword = "0987654321KnKn"

  val random = new Random

  val persons: List[Person] = for {
    i <- (1 to 10_000).toList
    name = s"Name$i"
    surname = s"Surname$i"
    brand = s"Brand$i"
    model = s"Model$i"
    year = 1970 + random.nextInt(50)
  } yield Person(name, surname, Car(brand, model, year))

  val mongoUrl = s"mongodb://$mongoUser:$mongoPassword@$mongoHost:$mongoPort"
  val program: Stream[IO, Unit] =
    for {
      conn <- Stream.resource(MongoClientImpl.fromUrl[IO](mongoUrl))
      database = conn.getDatabase("fs2practice")
      collection = database.getCollection("persons")
      person <- Stream.emits(persons)
      carDocument = new Document("brand", person.car.brand)
        .append("model", person.car.model)
        .append("year", person.car.year)
      personDocument = new Document("name", person.name)
        .append("surname", person.surname)
        .append("car", carDocument)
      _ <- Stream.emit(collection.insertOne(personDocument, callback))
    } yield ()

  program.compile.drain.unsafeRunSync()

  def callback[A]: SingleResultCallback[A] = {
    (result: A, throwable: Throwable) => {
      (Option(result), Option(throwable)) match {
        case (_, Some(t)) => println(s"Some error happened: ${t.getMessage}")
        case (r, None) => println("Document saved successfully")
      }
    }
  }

}
