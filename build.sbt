import sbt.Keys._

parallelExecution in ThisBuild := false

lazy val versions = new {
  val finatra = "2.10.0"
  val guice = "4.0"
  val logback = "1.1.7"
  val scalatest = "3.0.0"
  val specs2 = "2.4.17"
  val gatling = "2.1.7"
  val akka = "2.4.16"
}

lazy val baseSettings = Seq(
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.11.11",
  ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
  scalacOptions := Seq(
    "-encoding",
    "UTF-8",
    "-target:jvm-1.8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:implicitConversions",
    "-language:postfixOps"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  ),
  resolvers += Resolver.sonatypeRepo("releases"),
  fork in run := true,
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case meta(_)  => MergeStrategy.discard // or MergeStrategy.discard, your choice
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)

lazy val root = (project in file("."))
  .settings(
    name := "finatra-thrift-server-example",
    organization := "org.micchon",
    run := {
      (run in `server` in Compile).evaluated
    }
  )
  .aggregate(
    server,
    idl,
    loadtest
  )

lazy val server = (project in file("server"))
  .settings(baseSettings)
  .settings(
    name := "thrift-server",
    moduleName := "thrift-server",
    mainClass in (Compile, run) := Some("org.micchon.ExampleServerMain"),
    javaOptions ++= Seq(
      "-Dlog.service.output=/dev/stderr",
      "-Dlog.access.output=/dev/stderr"
    ),
    libraryDependencies ++= Seq(
      "com.twitter" %% "finatra-thrift" % versions.finatra,
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "com.twitter" %% "finatra-thrift" % versions.finatra % "test",
      "com.twitter" %% "inject-app" % versions.finatra % "test",
      "com.twitter" %% "inject-core" % versions.finatra % "test",
      "com.twitter" %% "inject-modules" % versions.finatra % "test",
      "com.twitter" %% "inject-server" % versions.finatra % "test",
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",
      "com.twitter" %% "finatra-thrift" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-app" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-server" % versions.finatra % "test" classifier "tests"
    )
  )
  .dependsOn(idl)

lazy val idl = (project in file("idl"))
  .settings(baseSettings)
  .settings(
    name := "thrift-idl",
    moduleName := "thrift-idl",
    scroogeThriftDependencies in Compile := Seq("finatra-thrift_2.11"),
    libraryDependencies ++= Seq(
      "com.twitter" %% "finatra-thrift" % versions.finatra
    )
  )

lazy val meta = """META.INF(.)*""".r

lazy val loadtest = (project in file("loadtest"))
  .enablePlugins(GatlingPlugin)
  .settings(baseSettings)
  .settings(
    name := "gatling-load-test",
    libraryDependencies ++= Seq(
      "io.gatling" % "gatling-app" % versions.gatling,
      "io.gatling.highcharts" % "gatling-charts-highcharts" % versions.gatling
        exclude("io.gatling", "gatling-recorder"),
      "io.gatling" % "gatling-test-framework" % versions.gatling,
      "com.typesafe.akka" %% "akka-stream" % versions.akka
    ),
    assemblyJarName in assembly := "gatling-loadtest.jar",
    mainClass in assembly := Some("io.gatling.thrift.testrunner.GatlingRunner")
  )
  .dependsOn(idl)