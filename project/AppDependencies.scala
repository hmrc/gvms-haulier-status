import play.core.PlayVersion.current
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.4.0"
  private val hmrcMongoVersion = "2.2.0"
  private val playVersion      = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion"    % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"           % hmrcMongoVersion,
    "uk.gov.hmrc"       %% s"internal-auth-client-$playVersion" % "3.0.0",
    "org.typelevel"     %% "cats-core"                          % "2.12.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion"  % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % Test
  )
}
