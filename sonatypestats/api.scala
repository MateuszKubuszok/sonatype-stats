package sonatypestats

import java.nio.file.*
import java.time.{YearMonth, ZoneOffset}

import sttp.client3.*
import sttp.model.*
import com.github.plokhotnyuk.jsoniter_scala.core.*

import scala.util.chaining.*

trait DerivedCodec[A] extends JsonValueCodec[A]
object DerivedCodec {

  inline def derived[A]: DerivedCodec[A] = new {
    import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
    private val inner = JsonCodecMaker.make[A]
    export inner.*
  }
}

case class Auth(username: String, password: String)
object Auth {
  given Auth = Auth(
    sys.env.getOrElse(
      "SONATYPE_USERNAME",
      sys.error("SONATYPE_USERNAME not set")
    ),
    sys.env.getOrElse(
      "SONATYPE_PASSWORD",
      sys.error("SONATYPE_PASSWORD not set")
    )
  )
}

case class IdName(id: String, name: String) derives DerivedCodec
case class IdNames(data: List[IdName]) derives DerivedCodec

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

  def getProjectIDsByName(using Auth): Result[Map[String, String]] = for {
    _ <- log(s"Fetching list of projects")
    json <- query(uri"https://oss.sonatype.org/service/local/stats/projects")
    result <- scala.util
      .Try {
        readFromString[IdNames](json).data.map(elem => elem.name -> elem.id).toMap
      }
      .toEither
      .left
      .map(_.getMessage)
  } yield result

  def getStats(using Auth)(
      name: String,
      projectID: String,
      organization: String,
      tpe: String,
      monthYear: YearMonth
  ): Result[String] = for {
    _ <- log(s"Fetching stats for $name $organization::$projectID at $monthYear ($tpe)")
    csv <- query {
      val year = monthYear.getYear
      val month = monthYear.getMonth.getValue
      uri"https://oss.sonatype.org/service/local/stats/$name?p=$projectID&g=$organization&a=&t=$tpe&from=${f"$year%04d$month%02d"}&nom=1"
    }
  } yield csv.trim // TODO: check if empty 
}
