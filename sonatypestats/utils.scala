package sonatypestats

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

extension [A](res: Result[A])
  def flatTap[B](f: A => Result[B]): Result[A] =
    res.flatMap(a => f(a).map(_ => a))
