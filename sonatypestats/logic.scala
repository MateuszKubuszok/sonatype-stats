package sonatypestats

import java.nio.file.*
import java.time.{YearMonth, ZoneOffset}

import scala.util.chaining.*

case class Row(downloads: String, uniquIps: String, timeline: String)

trait SonatypeCache {

  def read(path: String): Either[String, String]
  def write(path: String, value: String): Either[String, Unit]
}
object SonatypeCache {

  def default: SonatypeCache = new {
    import java.nio.file.*
    def read(path: String): Result[String] =
      scala.util
        .Try {
          Files.readString(Path.of(path))
        }
        .toEither
        .left
        .map(_ => "Cache was empty")
        .flatMap(c => if c.isEmpty() then Left("Cache was empty") else Right(c))
        .flatTap(_ => log(s"Read successfully from $path cache"))
    def write(path: String, value: String): Result[Unit] =
      scala.util
        .Try {
          if value.nonEmpty then {
            val file = Path.of(path)
            Files.createDirectories(file.getParent())
            Files.writeString(file, value)
            ()
          }
        }
        .toEither
        .left
        .map(_.getMessage())
        .flatTap(_ =>
          if value.nonEmpty then log(s"Wrote successfully to $path cache")
          else log(s"Skipped caching empty value to $path cache")
        )
  }
}

class SonatypeFetcher(using Auth)(
    // organization one was granted write access to
    projects: Set[String] = Set(
      sys.env.getOrElse(
        "SONATYPE_PROJECT",
        sys.error("SONATYPE_PROJECT not set")
      )
    ),
    // actual  organization used for publishing (must have proj as prefix)
    organization: String = sys.env.getOrElse(
      "SONATYPE_PROJECT",
      sys.error("SONATYPE_PROJECT not set")
    )
) {

  def fetchAll(cache: SonatypeCache = SonatypeCache.default): Result[Map[String, Map[YearMonth, Row]]] = {
    // data is generated after month has ended, with a several-days-long delay
    val lastMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1)
    val months = Iterator.iterate(lastMonth)(_.minusMonths(1L))

    def cacheOrFetch(
        name: String,
        projectID: String,
        organization: String,
        tpe: String,
        monthYear: YearMonth
    ): Result[String] = {
      val path = (name, tpe) match {
        case ("slices_csv", "ip")  => s"data/$organization/unique-ips/$monthYear.csv"
        case ("slices_csv", "raw") => s"data/$organization/downloads/$monthYear.csv"
        case ("timeline", _)       => s"data/$organization/total/$monthYear.json"
        case _                     => ???
      }
      cache
        .read(path)
        .orElse(
          SonatypeAPI
            .getStats(name, projectID, organization, tpe, monthYear)
            .flatTap(values => cache.write(path, values))
        )
    }

    for {
      _ <- log(s"Fetching everything from $lastMonth back for $projects and $organization")
      projectIDsByName <- SonatypeAPI.getProjectIDsByName
      result <- projectIDsByName.collect { case (name, id) if projects(name) => id }.iterator.traverse { projectID =>
        months
          .traverseWhile { month =>
            for {
              downloads <- cacheOrFetch("slices_csv", projectID, organization, "raw", month)
              uniqueIps <- cacheOrFetch("slices_csv", projectID, organization, "ip", month)
              timeline <- cacheOrFetch("timeline", projectID, organization, "ip", month)
              _ <-
                if (downloads.isEmpty() || uniqueIps.isEmpty() || timeline.isEmpty()) && month != lastMonth then
                  Left("No data for this month")
                else Right(())
            } yield month -> Row(downloads, uniqueIps, timeline)
          }
          .map(stats => projectID -> stats.toMap)
      }
    } yield result.toMap
  }
}
