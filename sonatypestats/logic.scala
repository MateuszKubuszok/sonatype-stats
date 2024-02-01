package sonatypestats

import java.nio.file._
import java.time.{YearMonth, ZoneOffset}

case class Row(downloads: String, uniquIps: String, timeline: String)

class SonatypeFetcher(
    auth: Auth,
    projects: Set[String],
    organization: String
  ) {

  def fetchAll(): Either[String, Map[String, Map[YearMonth, Row]]] = {
    val lastMonth = YearMonth.now(ZoneOffset.UTC)
    val firstMonth = lastMonth.minusMonths(4L) // TODO
    val months = Iterator.iterate(lastMonth)(_.minusMonths(1L)).takeWhile(_.compareTo(firstMonth) >= 0)

    println(s"Fetching everything from $firstMonth to $lastMonth for $projects and $organization")
    SonatypeAPI.getProjectIDsByName(auth).flatMap { projectIDsByName =>
      traverse(projectIDsByName.collect { case (name, id) if projects(name) => id }.iterator) { projectID =>
        traverse(months) { month =>
          for {
            downloads <- SonatypeAPI.getStats(auth)("slices_csv", projectID, organization, "raw", month)
            uniqueIps <- SonatypeAPI.getStats(auth)("slices_csv", projectID, organization, "ip", month)
            timeline  <- SonatypeAPI.getStats(auth)("timeline", projectID, organization, "ip", month)
          } yield month -> Row(downloads, uniqueIps, timeline)
        }.map(stats => projectID -> stats.toMap)
      }.map(_.toMap)
    }
  }

  private def traverse[A, B](it: Iterator[A])(f: A => Either[String, B]): Either[String, Vector[B]] = {
    val (errors, success) = it.map(f).toVector.partitionMap(identity)
    if (errors.isEmpty) Right(success) else Left(errors.mkString("\n"))
  }
}
