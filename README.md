# Scala Pet Store

> An implementation of the Java Pet Store using FP techniques in Scala  
> An analysis of [https://github.com/pauljamescleary/scala-pet-store](https://github.com/pauljamescleary/scala-pet-store), explained step by step

**Usage**

Every step is accessible via a respective tag, e.g. `step-2`, like this:

```bash
$ git checkout tags/step-2
```

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

## 3. Add database support

To utilize a database in a functional way we use Doobie and Hikari as transactor, which is transforming programs (descriptions of computations requiring a database connection) into computations that actually can be executed. First we extend our `/build.sbt` by adding Doobie, H2 and Hikari.

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
val DoobieVersion      = "0.6.0"
val H2Version          = "1.4.199"
val Http4sVersion      = "0.20.0-RC1"
val LogbackVersion     = "1.2.3"

libraryDependencies ++= Seq(
  "org.typelevel"  %% "cats-core"           % CatsVersion,
  "org.typelevel"  %% "cats-effect"         % CatsEffectVersion,
  "io.circe"       %% "circe-generic"       % CirceVersion,
  "io.circe"       %% "circe-config"        % CirceConfigVersion,
  "org.http4s"     %% "http4s-blaze-server" % Http4sVersion,
  "org.tpolecat"   %% "doobie-core"         % DoobieVersion,
  "org.tpolecat"   %% "doobie-hikari"       % DoobieVersion,
  "com.h2database" %  "h2"                  % H2Version,
  "ch.qos.logback" %  "logback-classic"     % LogbackVersion
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
```

Now we add the database configuration to `/src/main/resources/application.conf`.

```conf
petstore {
  database {
    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    user = "sa"
    password = ""
    driver = "org.h2.Driver"
    connections = {
      poolSize = 10
    }
  }
  server {
    host = "localhost"
    port = 8080
  }
}
```

To reflect it using Circe Config, we

* add `src/main/scala/io/m99/petstore/config/DatabaseConfig.scala`,
* extend `src/main/scala/io/m99/petstore/config/PetStoreConfig.scala`, and
* extend `src/main/scala/io/m99/petstore/config/package.scala`.  

Finally we can use the configuration to create a database transactor in `/src/main/scala/io.m99.petstore/Server.scala`.

```scala
package io.m99.petstore

import cats.effect._
import cats.syntax.functor._
import doobie.util.ExecutionContexts
import io.circe.config.parser
import io.m99.petstore.config.{DatabaseConfig, PetStoreConfig}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Server => H4Server}

object Server extends IOApp {

  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf             <- Resource.liftF(parser.decodePathF[F, PetStoreConfig]("petstore"))
      fixedThreadPool  <- ExecutionContexts.fixedThreadPool[F](conf.database.connections.poolSize)
      cachedThreadPool <- ExecutionContexts.cachedThreadPool[F]
      _                <- DatabaseConfig.transactor(conf.database, fixedThreadPool, cachedThreadPool)
      server <- BlazeServerBuilder[F]
        .bindHttp(conf.server.port, conf.server.host)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] =
    createServer.use(_ => IO.never).as(ExitCode.Success)
}
```
