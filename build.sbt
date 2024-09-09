import play.sbt.PlayImport.PlayKeys.playDefaultPort
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import sbt.Keys.evictionErrorLevel

lazy val microservice = Project("gvms-haulier-status", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    scalaVersion := "3.4.2",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
    playDefaultPort := 8990
  )
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId"
    )
  )
  .settings(
    scalafmtOnCompile := true
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(WartRemoverSettings.settings)
  .settings( // fix scaladoc generation in jenkins
    Compile / scalacOptions -= "utf8",
    scalacOptions += "-language:postfixOps"
  )

evictionErrorLevel := Level.Warn
