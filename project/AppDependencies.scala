import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._
import play.core.PlayVersion.current

object AppDependencies {

  private val bootstrapVersion = "7.13.0"
  private val hmrcMongoVersion = "1.3.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                 % "2.9.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapVersion % "test, it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % Test,
    "org.mockito"            % "mockito-core"             % "5.1.1"          % "test, it",
    "com.typesafe.play"      %% "play-test"               % current          % Test,
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0"       % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"          % "test, it",
  )
}
