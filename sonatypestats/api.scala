package sonatypestats

import java.nio.file.*
import java.time.{YearMonth, ZoneOffset}

import sttp.client3.*
import sttp.model.*

import scala.util.chaining.*

object SonatypeAPI {

  private val backend = HttpClientSyncBackend()

  private def query(using auth: Auth)(uri: Uri): Result[String] = {
    val r = basicRequest.auth
      .basic(auth.username, auth.password)
      .header("Accept", "application/json")
      .get(uri)
      .send(backend)

    r.body.left.map(e => s"Failed to query SonatypeAPI (${r.code}): $e")
  }

  def getProjectIDsByName(using Auth): Result[Map[ProjectName, ProjectID]] = for {
    _ <- log(s"Fetching list of projects")
    json <- query(uri"https://oss.sonatype.org/service/local/stats/projects")
    idNames <- decodeJson[IdNames](json)
  } yield idNames.data.map(elem => elem.name -> elem.id).toMap

  def getStats(using Auth)(
      name: String,
      projectID: ProjectID,
      organization: String,
      tpe: String,
      monthYear: YearMonth
  ): Result[String] = for {
    _ <- log(s"Fetching stats for $name $organization::$projectID at $monthYear ($tpe)")
    csvOrJson <- query {
      val year = monthYear.getYear
      val month = monthYear.getMonth.getValue
      uri"https://oss.sonatype.org/service/local/stats/$name?p=$projectID&g=$organization&a=&t=$tpe&from=${f"$year%04d$month%02d"}&nom=1"
    }
  } yield csvOrJson.trim
}
