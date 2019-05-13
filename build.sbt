name := "scala-pet-store-step-by-step"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-Ypartial-unification")

resolvers += Resolver.sonatypeRepo("snapshots")

val CatsVersion            = "1.6.0"
val CatsEffectVersion      = "1.2.0"
val CirceVersion           = "0.11.1"
val CirceConfigVersion     = "0.6.1"
val DoobieVersion          = "0.6.0"
val EnumeratumCirceVersion = "1.5.20"
val FlywayVersion          = "5.2.4"
val H2Version              = "1.4.199"
val Http4sVersion          = "0.20.0-RC1"
val LogbackVersion         = "1.2.3"
val ScalaCheckVersion      = "1.14.0"
val ScalaTestVersion       = "3.0.7"
val TsecVersion            = "0.1.0"

libraryDependencies ++= Seq(
  "org.typelevel"      %% "cats-core"           % CatsVersion,
  "org.typelevel"      %% "cats-effect"         % CatsEffectVersion,
  "io.circe"           %% "circe-generic"       % CirceVersion,
  "io.circe"           %% "circe-config"        % CirceConfigVersion,
  "com.beachape"       %% "enumeratum-circe"    % EnumeratumCirceVersion,
  "org.http4s"         %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s"         %% "http4s-circe"        % Http4sVersion,
  "org.http4s"         %% "http4s-dsl"          % Http4sVersion,
  "org.tpolecat"       %% "doobie-core"         % DoobieVersion,
  "org.tpolecat"       %% "doobie-hikari"       % DoobieVersion,
  "com.h2database"     % "h2"                   % H2Version,
  "org.flywaydb"       % "flyway-core"          % FlywayVersion,
  "ch.qos.logback"     % "logback-classic"      % LogbackVersion,
  "io.github.jmcardon" %% "tsec-common"         % TsecVersion,
  "io.github.jmcardon" %% "tsec-password"       % TsecVersion,
  "org.tpolecat"       %% "doobie-scalatest"    % DoobieVersion % Test,
  "org.http4s"         %% "http4s-blaze-client" % Http4sVersion % Test,
  "org.scalacheck"     %% "scalacheck"          % ScalaCheckVersion % Test,
  "org.scalatest"      %% "scalatest"           % ScalaTestVersion % Test
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
