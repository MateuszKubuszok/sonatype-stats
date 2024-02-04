import java.nio.file.*
import sonatypestats.*

def main(args: Array[String]): Unit = {
  val fetcher = new SonatypeFetcher()

  val result = for {
    data <- fetcher.fetchAll()
    _ <- log(s"Fetched all data successfully")
    (jsonFile, htmlFile) = renderStats(data)
    _ <- log(s"Updated $jsonFile and $htmlFile")
  } yield ()

  result match {
    case Left(value) => println(value)
    case Right(_)    => println("All operations finished successfully")
  }
}
