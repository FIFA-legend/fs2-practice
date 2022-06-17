package com.itechart.util

import com.mongodb.async.client.{MongoClient, MongoClients}
import com.mongodb.MongoClientSettings
import cats.effect.{Resource, Sync}

object MongoClientImpl {
  def fromUrl[F[_]](url: String)(implicit F: Sync[F]): Resource[F, MongoClient] =
    Resource.make(F.delay(MongoClients.create(url))) { client =>
      F.delay(client.close())
    }

  def fromSettings[F[_]](settings: MongoClientSettings)(implicit F: Sync[F]): Resource[F, MongoClient] = {
    Resource.make(F.delay(MongoClients.create(settings))) { client =>
      F.delay(client.close())
    }
  }
}
