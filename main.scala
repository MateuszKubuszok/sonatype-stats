import sonatypestats.*

def main(args: Array[String]): Unit = {
  val fetcher = new SonatypeFetcher()

  val result = for {
    data <- fetcher.fetchAll()
    _ <- log(s"Fetched all data successfully")
  } yield for {
    (projectName, monthToTimePoints) <- data
    _ = println(s"$projectName:")
    (month, TimePoint(downloads, uniqueIps, Timeline(_, _, _, totalAll, _))) <- monthToTimePoints
    _ = println(s"  $month:")
  } {
    println(s"    downloads:")
    for {
      TimePointDetail(name, total, fraction) <- downloads
    } println(s"      $name: $total ($fraction of $totalAll)")

    println(s"    unieque IPs:")
    for {
      TimePointDetail(name, total, fraction) <- uniqueIps
    } println(s"      $name: $total ($fraction of all)")
  }

  result match {
    case Left(value) => println(value)
    case Right(_)    => println("All operations finished successfully")
  }
}
