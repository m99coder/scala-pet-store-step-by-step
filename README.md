# Scala Pet Store

> An implementation of the Java Pet Store using FP techniques in Scala  
> An analysis of [https://github.com/pauljamescleary/scala-pet-store](https://github.com/pauljamescleary/scala-pet-store), explained step by step

## 1. Create the skeleton

We just use `sbt new m99coder/cats-minimal.g8` to create a minimal project skeleton with Cats Core 1.6.0 and Cats Effect 1.2.0. The project is based on this [template](https://github.com/m99coder/cats-minimal.g8).

## 2. Adjust the dependencies

We now add Circe (for JSON serialization) and Http4s (as the webserver) to our `/build.sbt`.

```scala
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
```
