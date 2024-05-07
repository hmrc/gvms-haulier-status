import play.sbt.routes.RoutesKeys.routes
import sbt.*
import sbt.Keys.*
import wartremover.Wart.*
import wartremover.WartRemover.autoImport.{wartremoverErrors, wartremoverExcluded, wartremoverWarnings}
import wartremover.Warts

object WartRemoverSettings {

  lazy val settings = {

    val warnings = {
      (Compile / compile / wartremoverWarnings) ++= Seq(
        Equals,
        JavaSerializable,
        ListAppend,
        SeqApply,
        ThreadSleep
      )
    }

    val errors = {
      (Compile / compile / wartremoverErrors) ++= Warts.allBut(
        Any,
        ListUnapply,
        Throw,
        ToString,
        ImplicitParameter,
        PublicInference,
        Nothing,
        Var,
        DefaultArguments,
        Equals,
        Overloading,
        FinalCaseClass,
        Serializable,
        Product,
        NonUnitStatements,
        PlatformDefault,
        StringPlusAny,
        ToString,
        JavaSerializable,
        ListAppend,
        SeqApply,
        ThreadSleep,
        While
      )
    }

    val routesAndFoldersExclusions = wartremoverExcluded ++= (Compile / routes).value

    val testExclusions = {
      val errorsExcluded = (Test / compile / wartremoverErrors) --= Seq(
        DefaultArguments,
        Serializable,
        Product,
        OptionPartial,
        GlobalExecutionContext
      )
      val warningsExcluded = (Test / compile / wartremoverWarnings) --= Seq(
        OptionPartial,
        Throw,
        Equals
      )
      errorsExcluded ++ warningsExcluded
    }

    warnings ++ errors ++ routesAndFoldersExclusions ++ testExclusions
  }
}
