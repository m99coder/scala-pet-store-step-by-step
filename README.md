# Functional Scala with Cats

Based on the [cats-minimal.g8][cats-minimal] forked from [cats-seed.g8][cats-seed] template by [Underscore][underscore].

Copyright Marco Lehmann <marco.lehmann@zalando.de>. Licensed [CC0 1.0][license].

## Getting Started

You will need to have Git, Java 8, and [SBT][sbt] installed. You will also need an internet connection to run the exercises. All other dependencies are either included with the repo or downloaded on demand during compilation.

Start SBT using the `sbt` command to enter SBT’s *interactive mode* (`>` prompt):

```bash
$ sbt
[info] Loading global plugins from <DIRECTORY>
[info] Loading settings for project scala-pet-store-build from plugins.sbt ...
[info] Loading project definition from <DIRECTORY>/project
[info] Updating ProjectRef(uri("file:<DIRECTORY>/project/"), "scala-pet-store-build")...
[info] Done updating.
[info] Loading settings for project scala-pet-store from build.sbt ...
[info] Set current project to cats-minimal (in build file:<DIRECTORY>)
[info] sbt server started
sbt:scala-pet-store>
```

From the SBT prompt you can run the code in `Main.scala`:

```bash
sbt:scala-pet-store> run

[info] Updating ...
[info] Done updating.
[info] Compiling 1 Scala source to <DIRECTORY>/target/scala-2.12/classes ...
[info] Done compiling.
[info] Packaging <DIRECTORY>/target/scala-2.12/scala-pet-store_2.12-0.0.1-SNAPSHOT.jar ...
[info] Done packaging.
[info] Running minimal.Main
Hello Cats!
[success] Total time: 1 s, completed Mar 29, 2019 4:47:43 PM
```

You can also start a *Scala console* (`scala>` prompt) to play with small snippets of code:

```bash
> console
[info] Starting scala interpreter...
Welcome to Scala 2.12.8 (OpenJDK 64-Bit Server VM, Java 1.8.0_202).
Type in expressions for evaluation. Or try :help.

scala> import cats.instances.string._, cats.syntax.semigroup._
import cats.instances.string._
import cats.syntax.semigroup._

scala> "Hello " |+| "Cats!"
res0: String = Hello Cats!

scala>
```

Press `Ctrl+D` to quit the Scala console and return to SBT interactive mode.

Press `Ctrl+D` again to quit SBT interactive mode and return to your shell.

[cats-minimal]: https://github.com/m99coder/cats-minimal.g8
[cats-seed]: https://github.com/underscoreio/cats-seed.g8
[underscore]: https://underscore.io
[license]: https://creativecommons.org/publicdomain/zero/1.0/
[sbt]: http://scala-sbt.org
