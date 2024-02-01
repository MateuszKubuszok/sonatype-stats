package sonatypestats

type Result[+A] = Either[String, A]

extension [A](it: Iterator[A])
  def traverse[B](f: A => Result[B]): Result[Vector[B]] = {
    val (errors, success) = it.map(f).toVector.partitionMap(identity)
    if errors.isEmpty then Right(success) else Left(errors.mkString("\n"))
  }

def log(msg: String): Result[Unit] = Right(println(msg))

extension [A](res: Result[A])
  def flatTap[B](f: A => Result[B]): Result[A] =
    res.flatMap(a => f(a).map(_ => a))
