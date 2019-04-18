name := "scala-pet-store"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-Ypartial-unification")

resolvers += Resolver.sonatypeRepo("snapshots")

val CatsVersion        = "1.6.0"
val CatsEffectVersion  = "1.2.0"
val CirceVersion       = "0.11.1"
val CirceConfigVersion = "0.6.1"
val Http4sVersion      = "0.20.0-RC1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core"           % CatsVersion,
  "org.typelevel" %% "cats-effect"         % CatsEffectVersion,
  "io.circe"      %% "circe-generic"       % CirceVersion,
  "io.circe"      %% "circe-config"        % CirceConfigVersion,
  "org.http4s"    %% "http4s-blaze-server" % Http4sVersion
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
