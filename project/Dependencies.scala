import sbt._

object Dependencies {

  object Codacy {
    val metricsSeed = "com.codacy" %% "codacy-metrics-scala-seed" % "0.1.247"
  }

  private val toolVersion = scala.io.Source.fromFile(".pmd-version").mkString.trim

  val specs2Version = "4.2.0"
  val specs2 = "org.specs2" %% "specs2-core" % specs2Version

  val pmd = Seq(
    "net.sourceforge.pmd" % "pmd-core" % toolVersion withSources (),
    "net.sourceforge.pmd" % "pmd-java" % toolVersion withSources ())
}
