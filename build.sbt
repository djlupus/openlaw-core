import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}
import ReleaseTransformations._

import scala.language.postfixOps
import sbt.{file, _}

name := "core"
organization := "org.openlaw"
homepage := Some(url("https://openlaw.io"))
lazy val username = "openlaw"
lazy val repo     = "openlaw-core"

lazy val scalaV = "2.12.7"
lazy val propellerV = "0.35"
lazy val catsV = "1.4.0"
lazy val parboiledV = "2.1.5"
lazy val circeV = "0.10.1"
lazy val akkaV = "2.5.17"

lazy val repositories = Seq(
  "nexus-snapshots" at "https://nexus.build.openlaw.io/repository/maven-snapshots",
  "nexus-releases" at "https://nexus.build.openlaw.io/repository/maven-releases",
  Resolver.jcenterRepo,
  "central" at "http://central.maven.org/maven2/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "cubefriendly bintray repository" at "http://dl.bintray.com/cubefriendly/maven",
  "ethereumj repository" at "http://dl.bintray.com/ethereum/maven",
  "maven central" at "https://mvnrepository.com/repos/central",
  "jitpack.io" at "https://jitpack.io",
  Resolver.mavenLocal
)

packageOptions += Package.ManifestAttributes(
  "Implementation-Version" -> (version in ThisBuild).value,
  "Implementation-Title" -> name.value
)

updateOptions := updateOptions.value.withCachedResolution(true)

scalacOptions ++= Seq("-Xlog-implicits", "-unchecked", "-deprecation", "-feature")
javacOptions ++= Seq("-Xms512M", "-Xmx1024M", "-Xss1M", "-XX:+CMSClassUnloadingEnabled")

lazy val dependencySettings = Seq(
  organization := "org.openlaw",
  scalaVersion := scalaV,
  // Add your sbt-dependency-check settings
  dependencyCheckOutputDirectory := Some(file("owasp"))
)

lazy val publishSettings = Seq(
  homepage := Some(url(s"https://github.com/$username/$repo")),
  licenses ++= Seq(("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))),
  bintrayOrganization := Some("openlaw"),
  bintrayRepository := "openlaw-core",
  bintrayPackageLabels := Seq("shared", "client"),
  scmInfo := Some(ScmInfo(url(s"https://github.com/$username/$repo"), s"git@github.com:$username/$repo.git")),
  releaseCrossBuild := true,
  developers := List(
    Developer(
      id = username,
      name = "David Roon",
      email = "david.roon@consensys.net",
      url = new URL(s"http://github.com/adridadou")
    ),
    Developer(
      id = username,
      name = "Jacqueline Outka",
      email = "jacqueline@outka.xyz",
      url = new URL(s"http://github.com/$username")
    )
  ),
  publishTo in ThisBuild := Some("Bintray" at "https://api.bintray.com/maven/openlaw/maven/openlaw-core")
)

lazy val releaseSettings = releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)

val rules = Seq(Wart.ArrayEquals, Wart.OptionPartial, Wart.EitherProjectionPartial, Wart.Enumeration, Wart.ExplicitImplicitTypes, Wart.FinalVal, Wart.JavaConversions, Wart.JavaSerializable, Wart.LeakingSealed)

lazy val openlawCoreJvm = (project in file("openlawCoreJvm")).settings(
  wartremoverErrors ++= rules,
  scalaVersion := scalaV,
  name := "openlaw-core",
  resolvers ++= repositories,
  libraryDependencies ++= Seq(
    //Test
    "org.scalacheck"          %% "scalacheck"          % "1.14.0"       % Test,
    "org.scalatest"           %% "scalatest"           % "3.0.6-SNAP2"  % Test,
    "org.mockito"             %  "mockito-all"         % "1.10.19"      % Test,
    "org.adridadou"           %% "eth-propeller-scala" % propellerV % Test,
  ),
  publishArtifact in (Compile, packageDoc) := false
).enablePlugins(WartRemover)
  .dependsOn(sharedJvm)
  .settings(dependencySettings: _*)
  .settings(publishSettings: _*)
  .settings(releaseSettings: _*)

lazy val openlawCoreJs = (project in file("openlawCoreJs")).settings(
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule)},
  scalaVersion := scalaV,
  name := "openlaw-core",
  resolvers ++= repositories,
  libraryDependencies ++= Seq(
    "org.scala-js"  %%% "scalajs-dom"             % "0.9.6",
  ),
  relativeSourceMaps := true,
  artifactPath in (Compile, fastOptJS) := crossTarget.value / "client.js",
  artifactPath in (Compile, fullOptJS) := crossTarget.value / "client.js"
).enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJs)
  .settings(dependencySettings: _*)
  .settings(publishSettings: _*)
  .settings(releaseSettings: _*)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure) // [Pure, Full, Dummy], default: CrossType.Full
  .in(file("shared"))
  .jvmSettings(
    wartremoverErrors ++= rules,
    resolvers ++= repositories,
    scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      "io.circe"                %% "circe-iteratee"      % "0.11.0",
      "io.iteratee"             %% "iteratee-monix"      % "0.18.0",
      "io.monix"                %% "monix-eval"          % "3.0.0-fbcb270",
      "io.monix"                %% "monix-execution"     % "3.0.0-fbcb270",
      "io.circe"                %% "circe-core"          % circeV,
      "io.circe"                %% "circe-generic"       % circeV,
      "io.circe"                %% "circe-parser"        % circeV,
      "com.typesafe.play"       %% "play-json"           % "2.6.10",
      "org.parboiled"           %% "parboiled"           % parboiledV,
      "org.typelevel"           %% "cats-core"           % catsV,
      "org.typelevel"           %% "cats-free"           % catsV,
      "io.github.cquiroz"       %% "scala-java-time"     % "2.0.0-M13",
      "biz.enef"                %% "slogging-slf4j"      % "0.6.1"
    )
  ).jsSettings(
    resolvers ++= repositories,
    scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      "io.github.cquiroz"       %%% "scala-java-time"      % "2.0.0-M13",
      "io.github.cquiroz"       %%% "scala-java-time-tzdb" % "2.0.0-M13_2018c",
      "org.parboiled"           %%% "parboiled"            % parboiledV,
      "com.chuusai"             %%% "shapeless"            % "2.3.3",
      "biz.enef"                %%% "slogging"             % "0.6.1",
      "org.typelevel"           %%% "cats-core"            % catsV,
      "org.typelevel"           %%% "cats-free"            % catsV,
      "io.circe"                %%% "circe-core"           % circeV,
      "io.circe"                %%% "circe-generic"        % circeV,
      "io.circe"                %%% "circe-parser"         % circeV,
      "com.typesafe.play"       %%% "play-json"            % "2.6.10",
    )
  )
  .settings(dependencySettings: _*)
  .enablePlugins(WartRemover)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js


