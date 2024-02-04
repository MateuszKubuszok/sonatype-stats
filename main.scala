import java.nio.file.*
import sonatypestats.*

def main(args: Array[String]): Unit = {
  val fetcher = new SonatypeFetcher()

  val result = for {
    data <- fetcher.fetchAll()
    _ <- log(s"Fetched all data successfully")
    dataJson = encodeJson(data)
    dataJsonPath = Path.of("data/data.json")
    _ = Files.writeString(dataJsonPath, dataJson)
    _ <- log(s"Overriden $dataJsonPath with:")
    _ <- log(encodeJson(data))
  } yield ()

  result match {
    case Left(value) => println(value)
    case Right(_)    => println("All operations finished successfully")
  }
}
