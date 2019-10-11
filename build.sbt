import com.typesafe.sbt.packager.docker.Cmd

name := "codacy-metrics-pmd"

lazy val toolVersion = settingKey[String]("The tool version")
toolVersion := scala.io.Source.fromFile(".pmd-version").mkString.trim

mappings in Universal ++= {
  (resourceDirectory in Compile) map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

ThisBuild / organization := "com.codacy"
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / scalacOptions in Test ++= Seq("-Yrangepos")
ThisBuild / scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings")
name := "codacy-metrics-pmd"
libraryDependencies ++= Seq(
  "com.codacy" %% "codacy-metrics-scala-seed" % "0.1.285",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "net.sourceforge.pmd" % "pmd-core" % toolVersion.value,
  "net.sourceforge.pmd" % "pmd-java" % toolVersion.value,
  "org.specs2" %% "specs2-core" % "4.7.1" % Test)
val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "openjdk:8-jre-alpine"

dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}")

dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("WORKDIR", _) => Seq(Cmd("WORKDIR", "/src"))
  case cmd @ Cmd("ADD", _) =>
    Seq(Cmd("RUN", s"adduser -u 2004 -D $dockerUser"), cmd, Cmd("RUN", "mv /opt/docker/docs /docs"))
  case other => List(other)
}
