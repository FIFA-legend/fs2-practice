package com.itechart.util

import com.mongodb.async.client.{MongoCollection, MongoIterable}
import fs2._
import cats.effect.Async
import cats.implicits._
import com.mongodb.async.{AsyncBatchCursor, SingleResultCallback}
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.WriteModel
import com.mongodb.client.result.UpdateResult
import org.bson.conversions.Bson

import scala.collection.JavaConverters._

object imports {
  final val Mongo = com.itechart.util.MongoClientImpl

  private[imports] implicit class AsyncToMongoOpt[A](val cb: Either[Throwable, Option[A]] => Unit)
      extends AnyVal {
    def toMongo: SingleResultCallback[A] = toMongo(identity)

    def toMongo[B](f: B => A): SingleResultCallback[B] = {
      new SingleResultCallback[B] {
        override def onResult(result: B, throwable: Throwable): Unit = {
          (Option(result), Option(throwable)) match {
            case (_, Some(t)) => cb(Left(t))
            case (r, None)    => cb(Right(r map f))
          }
        }
      }
    }
  }

  implicit class AsyncToMongo[A](val cb: Either[Throwable, A] => Unit) extends AnyVal {
    def toMongo: SingleResultCallback[A] = toMongo(identity)

    def toMongo[B](f: B => A): SingleResultCallback[B] = {
      new SingleResultCallback[B] {
        override def onResult(result: B, throwable: Throwable): Unit = {
          (result, Option(throwable)) match {
            case (_, Some(t)) => cb(Left(t))
            case (r, None)    => cb(Right(f(r)))
          }
        }
      }
    }
  }

  implicit class MongoIterableSyntax[A, B](iterable: A)(implicit ev: A <:< MongoIterable[B]) {
    private def asyncNext[F[_], T](cursor: AsyncBatchCursor[T])(
        implicit A: Async[F]): F[Option[Seq[T]]] = {
      if (cursor.isClosed) {
        A.pure(None)
      } else {
        A.async { cb =>
          Option(cursor.next(cb.toMongo(_.asScala.toSeq)).pure[F]).pure[F]
        }
      }
    }

    private def closeCursor[F[_]](maybeCursor: Option[AsyncBatchCursor[_]])(
        implicit A: Async[F]): F[Unit] =
      maybeCursor.fold(A.pure(()))(cursor => A.delay(cursor.close()))

    private def iterate[F[_]: Async](maybeCursor: Option[AsyncBatchCursor[B]]): Stream[F, B] = {
      maybeCursor match {
        case None =>
          Stream.empty

        case Some(cursor) =>
          Stream
            .repeatEval(asyncNext(cursor))
            .unNoneTerminate
            .flatMap(values => Stream.chunk(Chunk.seq(values)))
      }
    }

    private def asyncBatchCursor[F[_]](implicit A: Async[F]): F[Option[AsyncBatchCursor[B]]] = {
      //A.suspend {
        A.async { cb: (Either[Throwable, Option[AsyncBatchCursor[B]]] => Unit) =>
          Option(ev(iterable).batchCursor(cb.toMongo).pure[F]).pure[F]
        }
      //}
    }

    def stream[F[_]: Async]: Stream[F, B] = {
      Stream.bracket(asyncBatchCursor[F])(closeCursor[F]).flatMap(iterate[F])
    }
  }

  implicit class MongoCollectionSyntax[A](collection: MongoCollection[A]) {
    def effect[F[_]]: MongoCollectionEffect[F, A] = new MongoCollectionEffect[F, A](collection)
  }
}

class MongoCollectionEffect[F[_], A](val underlying: MongoCollection[A]) extends AnyVal {
  import imports.AsyncToMongo

  def bulkWrite(requests: List[WriteModel[A]])(implicit F: Async[F]): F[BulkWriteResult] = {
    Async[F]
      .async[BulkWriteResult] { cb =>
        Option(underlying.bulkWrite(requests.asJava, cb.toMongo).pure[F]).pure[F]
      }
  }

  def count(implicit F: Async[F]): F[Long] = {
    Async[F]
      .async[java.lang.Long] { cb =>
        Option(underlying.countDocuments(cb.toMongo).pure[F]).pure[F]
      }
      .map(_.longValue())
  }

  def count(filter: Bson)(implicit F: Async[F]): F[Long] = {
    Async[F]
      .async[java.lang.Long] { cb =>
        Option(underlying.countDocuments(filter, cb.toMongo).pure[F]).pure[F]
      }
      .map(_.longValue())
  }

  def insertOne(document: A)(implicit F: Async[F]): F[Unit] = {
    Async[F]
      .async[Void] { cb =>
        Option(underlying.insertOne(document, cb.toMongo).pure[F]).pure[F]
      }
      .void
  }

  def insertMany(documents: Seq[A])(implicit F: Async[F]): F[Unit] = {
    Async[F]
      .async[Void] { cb =>
        Option(underlying.insertMany(documents.asJava, cb.toMongo).pure[F]).pure[F]
      }
      .void
  }

  def updateOne(filter: Bson, update: Bson)(implicit F: Async[F]): F[UpdateResult] = {
    Async[F]
      .async[UpdateResult] { cb =>
        Option(underlying.updateOne(filter, update, cb.toMongo).pure[F]).pure[F]
      }
  }

  def updateMany(filter: Bson, update: Bson)(implicit F: Async[F]): F[UpdateResult] = {
    Async[F]
      .async[UpdateResult] { cb =>
        Option(underlying.updateMany(filter, update, cb.toMongo).pure[F]).pure[F]
      }
  }
}
