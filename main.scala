import sonatypestats._


def auth: String = Auth(
  sys.env.getOrElse(
    "SONATYPE_USERNAME",
    sys.error("SONATYPE_USERNAME not set")
  ),
  sys.env.getOrElse(
    "SONATYPE_PASSWORD",
    sys.error("SONATYPE_PASSWORD not set")
  )
)

// organization one was granted write access to
def projects: String = Set(sys.env.getOrElse(
  "SONATYPE_PROJECT",
  sys.error("SONATYPE_PROJECT not set")
))

// actual organization used for publishing (must have proj as prefix)
def organization: String = sys.env.getOrElse("SONATYPE_PROJECT", sys.error("SONATYPE_PROJECT not set"))

def main(args: Array[String]): Unit = {
  val fetcher = new SonatypeFetcher(auth, projects, organization)

  val result = for {
    result <- fetcher.fetchAll()
    _ = println(s"Fetched all data successfully")

  } yield ()

  println(result)
}

