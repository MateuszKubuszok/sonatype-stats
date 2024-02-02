package sonatypestats

import com.github.gekomad.ittocsv.core.FromCsv
import com.github.plokhotnyuk.jsoniter_scala.core.*

sealed trait DerivedCodec[A] extends JsonValueCodec[A]
object DerivedCodec {

  inline def derived[A]: DerivedCodec[A] = new {
    import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
    private val inner = JsonCodecMaker.make[A]
    export inner.*
  }
}

sealed trait DerivedCvsDecoder[A] extends FromCsv.Decoder[String, List[A]]
object DerivedCvsDecoder {
  inline def derived[A](using
      m: scala.deriving.Mirror.ProductOf[A],
      d: FromCsv.Decoder[List[String], m.MirroredElemTypes]
  ): DerivedCvsDecoder[A] =
    new {
      def apply(v1: String): Either[List[String], List[A]] = {
        import com.github.gekomad.ittocsv.parser.{Constants, IttoCSVFormat}
        val (errors, values) = FromCsv
          .fromCsv(v1)(using
            m,
            d,
            IttoCSVFormat.default.withRecordSeparator(Constants.LF).withIgnoreEmptyLines(true)
          )
          .partitionMap(identity)
        if errors.isEmpty then Right(values) else Left(errors.flatten)
      }
    }
}

type Result[+A] = Either[String, A]

extension [A](it: Iterator[A])
  def traverse[B](f: A => Result[B]): Result[Vector[B]] = {
    val (errors, success) = it.map(f).toVector.partitionMap(identity)
    if errors.isEmpty then Right(success) else Left(errors.mkString("\n"))
  }

  def traverseWhile[B](f: A => Result[B]): Result[Vector[B]] = {
    var firstWasError = false
    it.map(f)
      .zipWithIndex
      .takeWhile {
        case (Left(_), 0) =>
          firstWasError = true
          true
        case (Right(_), _) if !firstWasError => true
        case _                               => false
      }
      .toVector match {
      case Vector((Left(error), 0)) => Left(error)
      case result                   => Right(result.collect { case (Right(value), _) => value })
    }
  }

def log(msg: String): Result[Unit] = Right(println(msg))

def decodeCsv[A](csv: String)(using d: FromCsv.Decoder[String, List[A]]): Result[Vector[A]] =
  d(csv).left.map(_.mkString("\n")).map(_.toVector)

def decodeJson[A: JsonValueCodec](json: String): Result[A] =
  scala.util
    .Try(readFromString[A](json))
    .toEither
    .left
    .map(_.getMessage)

extension [A](res: Result[A])
  def flatTap[B](f: A => Result[B]): Result[A] =
    res.flatMap(a => f(a).map(_ => a))
