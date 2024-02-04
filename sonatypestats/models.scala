package sonatypestats

import java.time.YearMonth

import com.github.gekomad.ittocsv.core.FromCsv

final case class Auth(username: String, password: String)
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

type ProjectID = String // TODO: opaque type
type ProjectName = String // TODO: opaque type
type ArtifactName = String // TODO: opaque type

final case class IdName(id: ProjectID, name: ProjectName) derives DerivedCodec
final case class IdNames(data: List[IdName]) derives DerivedCodec
final case class Timeline(projectId: ProjectID, groupId: String, `type`: String, total: Long, timeline: List[Long])
    derives DerivedCodec
final case class TimelineData(data: Timeline) derives DerivedCodec

final case class TimePointDetail(name: ArtifactName, downloads: String, fractionOfAll: String)
    derives DerivedCodec,
      DerivedCvsDecoder
final case class TimePoint(
    downloads: Map[ArtifactName, String],
    uniqueIps: Map[ArtifactName, String],
    timeline: Timeline
)

final case class CompleteData(data: Map[ProjectName, Map[YearMonth, TimePoint]]) derives DerivedCodec
