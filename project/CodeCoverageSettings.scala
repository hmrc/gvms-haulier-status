import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedFiles := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;" +
      "app.*;.*BuildInfo.*;.*Routes.*;.*repositories.*;.*controllers.test.*;.*connectors.test.*;.*connectors.analytics.*;.*services.test.*;.*controllers.package.*;.*documents.*;.*metrics.*;" +
      ".*views.html.*;Reverse.*;.*models.api.GoodsMovementRecordData;.*models.api.metadata.*;.*Module;.*StatusCheckScheduler",
    ScoverageKeys.coverageExcludedPackages :=
      ("uk.gov.hmrc.gvmshaulierstatus.controllers.test.*;" +
        "uk.gov.hmrc.gvmshaulierstatus.services.test.*;" +
        "uk.gov.hmrc.gvmshaulierstatus.connectors.test.*;" +
        "uk.gov.hmrc.gvmshaulierstatus.models.*;"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
