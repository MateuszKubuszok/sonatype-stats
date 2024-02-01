package sonatypestats

import java.nio.file._
import java.time.{YearMonth, ZoneOffset}

import sttp.client3._
import sttp.model._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import scala.util.chaining._

case class Auth(username: String, password: String)

case class IdName(id: String, name: String)
object IdName {
  implicit val idNameCodec: JsonValueCodec[IdName] = JsonCodecMaker.make[IdName]
  implicit val listIdNameCodec: JsonValueCodec[List[IdName]] = JsonCodecMaker.make[List[IdName]]
}

case class IdNames(data: List[IdName])
object IdNames {
  implicit val idNamesCodec: JsonValueCodec[IdNames] = JsonCodecMaker.make[IdNames]
}

case class UniqueIpData(total: Int)
object UniqueIpData {
  implicit val uniqueIpDataCodec: JsonValueCodec[UniqueIpData] = JsonCodecMaker.make[UniqueIpData]
}

case class UniqueIpResp(data: UniqueIpData)
object UniqueIpResp {
  implicit val uniqueIpRespCodec: JsonValueCodec[UniqueIpResp] = JsonCodecMaker.make[UniqueIpResp]
}

object SonatypeAPI {

  private val backend = HttpClientSyncBackend()

  private def query(auth: Auth, uri: Uri): Either[String, String] = {
    val r = basicRequest.auth.basic(auth.username, auth.password)
      .header("Accept", "application/json")
      .get(uri)
      .send(backend)

    r.body.left.map(e => s"Failed to query SonatypeAPI (${r.code}): ${e}")
  }

  def getProjectIDsByName(auth: Auth): Either[String, Map[String, String]] = {
    println(s"Fetching list of projects")
    query(auth, uri"https://oss.sonatype.org/service/local/stats/projects").flatMap { json =>
      scala.util.Try {
        readFromString[IdNames](json).data.map(elem => elem.name -> elem.id).toMap
      }.toEither.left.map(_.getMessage)
    }
  }

  def getStats(auth: Auth)(
    name: String,
    projectId: String,
    organization: String,
    tpe: String,
    monthYear: YearMonth
  ): Either[String, String] = {
    println(s"Fetching stats for $name $organization::$projectId at $monthYear ($tpe)")
    query(auth, {
      val year = monthYear.getYear
      val month = monthYear.getMonth.getValue
      uri"https://oss.sonatype.org/service/local/stats/$name?p=$projectId&g=$organization&a=&t=$tpe&from=${f"$year%04d$month%02d"}&nom=1"
    }).map(_.trim) // TODO: check if empty
  }
}