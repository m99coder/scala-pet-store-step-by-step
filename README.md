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

* add `config/DatabaseConfig.scala`,
* extend `config/PetStoreConfig.scala`, and
* extend `config/package.scala`.  

Finally we can use the configuration to create a database transactor in `Server.scala`.

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

## 4. Apply database migrations

Database migrations are driven by Flyway, so we add `flyway-core` version `5.2.4` to our `/build.sbt`. The migrations itself are created within the `/src/main/resources/db/migration` folder and follow a certain versioning schema. To actually run the migrations we add the `initializeDb` method to `config/DatabaseConfig.scala`.

```scala
def initializeDb[F[_]](config: DatabaseConfig)(implicit S: Sync[F]): F[Unit] =
  S.delay {
      val fw: Flyway = {
        Flyway.configure().dataSource(config.url, config.user, config.password).load()
      }
      fw.migrate()
    }
    .as(())
``` 

Finally we execute the migrations in the for-comprehension of our `createServer` method in `Server.scala`.

```scala
_ <- Resource.liftF(DatabaseConfig.initializeDb(conf.database))
```

## 5. Add domain object, algebra and interpreter

First we add our domain object `Pet` in `domain/pets/Pet.scala` and an Algebraic Data Type (ADT) for the `status` property in `domain/pets/PetStatus.scala`. For the ADT we use a library called `enumeratum`, which we need to add to `/build.sbt`.

```scala
val EnumeratumCirceVersion = "1.5.20"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum-circe" % EnumeratumCirceVersion
)
```

The algebra defines the API we offer to interact with our domain object `Pet`. We define it in the tagless final manner using `F` in `domain/pets/PetRepositoryAlgebra.scala`.

```scala
package io.m99.petstore.domain.pets

trait PetRepositoryAlgebra[F[_]] {
  def create(pet: Pet): F[Pet]
  def update(pet: Pet): F[Option[Pet]]
  def get(id: Long): F[Option[Pet]]
  def delete(id: Long): F[Option[Pet]]
}
```

To connect the algebra with a concrete interpreter we create `infrastructure/repository/doobie/DoobiePetRepositoryInterpreter.scala` and use this one in our `Server.scala`.

```scala
transactor <- DatabaseConfig.transactor(conf.database, fixedThreadPool, cachedThreadPool)
_          = DoobiePetRepositoryInterpreter[F](transactor)
```

## 6. Validation

We want to separate logical errors from business errors, which are modeled with their own ADT. Therefore we create `domain/ValidationError.scala`.

```scala
package io.m99.petstore.domain

sealed trait ValidationError extends Product with Serializable

case object PetNotFoundError extends ValidationError
```

Now we create an algebra in `domain/pets/PetValidationAlgebra.scala` and an interpreter of this algebra in `domain/pets/PetValidationInterpreter.scala` for handling our business errors. As with the previous steps we finally include our validation interpreter into the for-comprehension of `Server.scala`.

```scala
petRepository = DoobiePetRepositoryInterpreter[F](transactor)
_             = PetValidationInterpreter[F](petRepository)
```

## 7. Service

The service is the entry point to our domain. It works with the provided repository and validation algebras to implement behavior. The only file we add is `domain/pets/PetService.scala`.

```scala
package io.m99.petstore.domain.pets

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import io.m99.petstore.domain.PetNotFoundError

class PetService[F[_]](repositoryAlgebra: PetRepositoryAlgebra[F],
                       validationAlgebra: PetValidationAlgebra[F]) {
  def create(pet: Pet): F[Pet] = repositoryAlgebra.create(pet)
  def update(pet: Pet)(implicit M: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] =
    for {
      _     <- validationAlgebra.doesNotExist(pet.id)
      saved <- EitherT.fromOptionF(repositoryAlgebra.update(pet), PetNotFoundError)
    } yield saved
  def get(id: Long)(implicit M: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] =
    EitherT.fromOptionF(repositoryAlgebra.get(id), PetNotFoundError)
  def delete(id: Long)(implicit M: Monad[F]): F[Unit] = repositoryAlgebra.delete(id).as(())
}

object PetService {
  def apply[F[_]: Monad](repositoryAlgebra: PetRepositoryAlgebra[F],
                         validationAlgebra: PetValidationAlgebra[F]) =
    new PetService[F](repositoryAlgebra, validationAlgebra)
}
```

Now we can use it again in the for-comprehension of our `Server.scala`.

```scala
petRepository = DoobiePetRepositoryInterpreter[F](transactor)
petValidation = PetValidationInterpreter[F](petRepository)
_             = PetService[F](petRepository, petValidation)
```

## 8. Endpoint

The most important part of creating the endpoint is located in `infrastructure/endpoint/PetEndpoints.scala`, which contains endpoint for different HTTP methods utilizing the provided `PetService`. To connect all the dots, change `Server.scala` like this.

```scala
services = PetEndpoints.endpoints[F](petService)
httpApp  = Router("/" -> services).orNotFound
_ <- Resource.liftF(DatabaseConfig.initializeDb(conf.database))
server <- BlazeServerBuilder[F]
  .bindHttp(conf.server.port, conf.server.host)
  .withHttpApp(httpApp)
  .resource
```

Now you can interact with the application using `curl` like shown below.

```bash
$ # Creating a pet
$ curl -i \
    -H "Content-Type: application/json" \
    -d '{
          "category": "Cat",
          "bio": "I am fuzzy",
          "name": "Harry",
          "tags": [],
          "photoUrls": [],
          "status": "Available"
        }' \
    -X POST http://localhost:8080/pets
    
$ # Getting a pet
$ curl -i http://localhost:8080/pets/1

$ # Updating a pet
$ curl -i \
    -H "Content-Type: application/json" \
    -d '{
          "category": "Cat",
          "bio": "I am fuzzy",
          "name": "Harry",
          "tags": [],
          "photoUrls": [],
          "status": "Pending"
        }' \
    -X PUT http://localhost:8080/pets/1
    
$ # Deleting a pet
$ curl -i -X DELETE http://localhost:8080/pets/1
```

## 9. Extend algebra and validations

Now we extend our logic by three different parts:

1. We add a new validation for an already existing pet based on name and category to `domain/ValidationError.scala` and adjust our validation algebra and interpreter accordingly
2. We extend our algebra with a couple of convenience methods which will be used by our service and adjust the Doobie interpreter accordingly
3. We finally also adjust our endpoint for creating a pet and add a list endpoint

You can easily demo that the added logic works for case 1 by creating the same pet twice, which now results in a 409 Conflict response and for case 3 by calling `http://localhost:8080/pets` before and after having created a pet.

## 10. Pagination and query parameters

We extend the listing endpoint of pets with optional query parameters for limiting the number of retrieved pets and specifying the offset as well. Therefore `infrastructure/repository/doobie/SQLPagination.scala` was added and incorporated into the domain logic. Additionally `infrastructure/endpoint/Pagination.scala` defines the query parameter decoders used in the endpoint. Finally we add two new endpoints which can be used to retrieve pets by status and tag. Both support multiple occurrences of the defined query parameter identifiers, `status` and `tag`.

```bash
$ # query pets with custom limit and offset
$ curl -i http://localhost:8080/pets\?limit\=2\&offset\=2

$ # query pets by status
$ curl -i http://localhost:8080/pets/findByStatus\?status\=Pending\&status\=Available

$ # query pets by tag
$ curl -i http://localhost:8080/pets/findByTag\?tag\=goldie\&tag\=labrador
```

## 11. Orders

Now we add our second domain object `Order` with all the components required. Be aware that the pet needs to exist in the database before you create an order referencing it, otherwise your query will result in an Internal Server Error.

```bash
$ # Creating an order
$ curl -i \
    -H "Content-Type: application/json" \
    -d '{
          "petId": 1,
          "status": "Placed",
          "complete": false
        }' \
    -X POST http://localhost:8080/orders
    
$ # Getting an order
$ curl -i http://localhost:8080/orders/1

$ # Deleting an order
$ curl -i -X DELETE http://localhost:8080/orders/1
```

## 12. Users

Now we add our third and last domain object `User` with all the components required. For now we don’t care about security, in the sense of storing the password in plain text. We will fix that in the next step.

```bash
$ # Creating a user
$ curl -i \
    -H "Content-Type: application/json" \
    -d '{
          "userName": "johndoe",
          "firstName": "John",
          "lastName": "Doe",
          "email": "johndoe@example.com",
          "password": "s3cr3t"
        }' \
    -X POST http://localhost:8080/users
    
$ # Getting a user
$ curl -i http://localhost:8080/users/johndoe

$ # Deleting a user
$ curl -i -X DELETE http://localhost:8080/users/johndoe
```

## 13. Security and Authentication

To apply proper security we hash the provided password using the `tsec` library. If you re-run the `curl` command to create a user you will recognize the hash in the response.

```bash
$ # Login with correct credentials
$ curl -i \
    -H "Content-Type: application/json" \
    -d '{
          "userName": "johndoe",
          "password": "s3cr3t"
        }' \
    -X POST http://localhost:8080/login
    
$ # Login with incorrect credentials
$ curl -i \
    -H "Content-Type: application/json" \
    -d '{
          "userName": "johndoe",
          "password": "foobar"
        }' \
    -X POST http://localhost:8080/login
```
