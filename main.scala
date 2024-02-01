import sonatypestats.*

def main(args: Array[String]): Unit = {
  val fetcher = new SonatypeFetcher()

  val result = for {
    _ <- fetcher.fetchAll()
    _ <- log(s"Fetched all data successfully")
  } yield ()

  println(result)
}
