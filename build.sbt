import ReleaseTransformations._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import sbtrelease.ReleasePlugin
import scala.sys.process.Process
import sbtcrossproject.crossProject
import java.io.{File => JFile}

lazy val baseModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-core-portable-jvm`,
  `quill-core-jvm`,
  `quill-sql-portable-jvm`,
  `quill-sql-jvm`
)

lazy val jasyncModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-jasync`, `quill-jasync-mysql`
)

lazy val asyncModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
//  `quill-async`, `quill-async-mysql`
) ++ jasyncModules

lazy val allModules = baseModules

lazy val scala213Modules = baseModules ++ Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-jasync`,
  `quill-jasync-mysql`
)

def isScala213 = {
  val scalaVersion = sys.props.get("quill.scala.version")
  scalaVersion.map(_.startsWith("2.13")).getOrElse(false)
}

val filteredModules = {
  val modulesStr = sys.props.get("modules")
  println(s"Modules Argument Value: ${modulesStr}")

  val modules = modulesStr match {
    case Some("base") =>
      println("Compiling Base Modules")
      baseModules
    case Some("async") =>
      println("Compiling Async Database Modules")
      asyncModules
    case Some("none") =>
      println("Invoking Aggregate Project")
      Seq[sbt.ClasspathDep[sbt.ProjectReference]]()
    case _ =>
      // Workaround for https://github.com/sbt/sbt/issues/3465
      val scalaVersion = sys.props.get("quill.scala.version")
      if(scalaVersion.map(_.startsWith("2.13")).getOrElse(false)) {
        println("Compiling Scala 2.13 Modules")
        baseModules ++ jasyncModules
      } else {
        println("Compiling All Modules")
        allModules
      }
  }
  if(isScala213) {
    println("Compiling 2.13 Modules Only")
    modules.filter(scala213Modules.contains(_))
  } else modules
}

lazy val `quill` =
  (project in file("."))
    .settings(commonSettings: _*)
    .aggregate(filteredModules.map(_.project): _*)
    .dependsOn(filteredModules: _*)

publishArtifact in `quill` := false

lazy val superPure = new sbtcrossproject.CrossType {
  def projectDir(crossBase: File, projectType: String): File =
    projectType match {
      case "jvm" => crossBase
      case "js"  => crossBase / s".$projectType"
    }

  def sharedSrcDir(projectBase: File, conf: String): Option[File] =
    Some(projectBase.getParentFile / "src" / conf / "scala")

  override def projectDir(crossBase: File, projectType: sbtcrossproject.Platform): File =
    projectType match {
      case JVMPlatform => crossBase
      case JSPlatform  => crossBase / ".js"
    }
}

def pprintVersion(v: String) = {
  if(v.startsWith("2.11")) "0.5.4" else "0.5.5"
}

lazy val `quill-core-portable` =
  crossProject(JVMPlatform, JSPlatform).crossType(superPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe"               %  "config"        % "1.4.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.scala-lang"             %  "scala-reflect" % scalaVersion.value
    ))
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "pprint" % pprintVersion(scalaVersion.value),
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5",
        "com.lihaoyi" %%% "pprint" % "0.5.4",
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5"
      ),
      coverageExcludedPackages := ".*"
    )

lazy val `quill-core-portable-jvm` = `quill-core-portable`.jvm

lazy val `quill-core` =
  crossProject(JVMPlatform, JSPlatform).crossType(superPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe"               %  "config"        % "1.4.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.scala-lang"             %  "scala-reflect" % scalaVersion.value
    ))
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "pprint" % pprintVersion(scalaVersion.value),
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5",
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5"
      ),
      coverageExcludedPackages := ".*"
    )
    .dependsOn(`quill-core-portable` % "compile->compile")

lazy val `quill-core-jvm` = `quill-core`.jvm

lazy val `quill-sql-portable` =
  crossProject(JVMPlatform, JSPlatform).crossType(superPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.vertical-blank"  %% "scala-sql-formatter" % "1.0.0"
    ))
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.github.vertical-blank" %%% "scala-sql-formatter" % "1.0.0"
      ),
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
      coverageExcludedPackages := ".*"
    )
    .dependsOn(`quill-core-portable` % "compile->compile")


lazy val `quill-sql-portable-jvm` = `quill-sql-portable`.jvm


lazy val `quill-sql` =
  crossProject(JVMPlatform, JSPlatform).crossType(superPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.vertical-blank"  %% "scala-sql-formatter" % "1.0.1"
    ))
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.github.vertical-blank" %%% "scala-sql-formatter" % "1.0.1"
      ),
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
      coverageExcludedPackages := ".*"
    )
    .dependsOn(
      `quill-sql-portable` % "compile->compile",
      `quill-core` % "compile->compile;test->test"
    )



lazy val `quill-sql-jvm` = `quill-sql`.jvm

val excludeTests =
  sys.props.getOrElse("excludeTests", "false").toBoolean

val skipPush =
  sys.props.getOrElse("skipPush", "false").toBoolean

val debugMacro =
  sys.props.getOrElse("debugMacro", "false").toBoolean

val skipTag =
  sys.props.getOrElse("skipTag", "false").toBoolean

lazy val `quill-async` =
  (project in file("quill-async"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.postgresql-async" %% "db-async-common"  % "0.3.0"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")

lazy val `quill-async-mysql` =
  (project in file("quill-async-mysql"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.postgresql-async" %% "mysql-async"      % "0.3.0"
      )
    )
    .dependsOn(`quill-async` % "compile->compile;test->test")

lazy val `quill-async-postgres` =
  (project in file("quill-async-postgres"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.postgresql-async" %% "postgresql-async" % "0.3.0"
      )
    )
    .dependsOn(`quill-async` % "compile->compile;test->test")

lazy val `quill-jasync` =
  (project in file("quill-jasync"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.jasync-sql" % "jasync-common" % "1.0.17",
        "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")

lazy val `quill-jasync-postgres` =
  (project in file("quill-jasync-postgres"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.jasync-sql" % "jasync-postgresql" % "1.0.17"
      )
    )
    .dependsOn(`quill-jasync` % "compile->compile;test->test")

lazy val `quill-jasync-mysql` =
  (project in file("quill-jasync-mysql"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.jasync-sql" % "jasync-mysql" % "1.0.17"
      )
    )
    .dependsOn(`quill-jasync` % "compile->compile;test->test")

lazy val mimaSettings = MimaPlugin.mimaDefaultSettings ++ Seq(
  mimaPreviousArtifacts := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor <= 11 =>
        Set(organization.value % s"${name.value}_${scalaBinaryVersion.value}" % "0.5.0")
      case _ =>
        Set()
    }
  }
)

commands += Command.command("checkUnformattedFiles") { st =>
  val vcs = Project.extract(st).get(releaseVcs).get
  val modified = vcs.cmd("ls-files", "--modified", "--exclude-standard").!!.trim.split('\n').filter(_.contains(".scala"))
  if(modified.nonEmpty)
    throw new IllegalStateException(s"Please run `sbt scalariformFormat test:scalariformFormat` and resubmit your pull request. Found unformatted files: ${modified.toList}")
  st
}

def updateReadmeVersion(selectVersion: sbtrelease.Versions => String) =
  ReleaseStep(action = st => {

    val newVersion = selectVersion(st.get(ReleaseKeys.versions).get)

    import scala.io.Source
    import java.io.PrintWriter

    val pattern = """"io.getquill" %% "quill-.*" % "(.*)"""".r

    val fileName = "README.md"
    val content = Source.fromFile(fileName).getLines.mkString("\n")

    val newContent =
      pattern.replaceAllIn(content,
        m => m.matched.replaceAllLiterally(m.subgroups.head, newVersion))

    new PrintWriter(fileName) { write(newContent); close }

    val vcs = Project.extract(st).get(releaseVcs).get
    vcs.add(fileName).!

    st
  })

def updateWebsiteTag =
  ReleaseStep(action = st => {

    val vcs = Project.extract(st).get(releaseVcs).get
    vcs.tag("website", "update website", false).!

    st
  })


def excludePaths(paths:Seq[String]) = {
  val excludeThisPath =
    (path: String) =>
      paths.exists { srcDir =>
        (path contains srcDir)
      }
  new SimpleFileFilter(file => {
    if (excludeThisPath(file.getCanonicalPath))
      println(s"Excluding: ${file.getCanonicalPath}")
    excludeThisPath(file.getCanonicalPath)
  })
}

lazy val basicSettings = Seq(
  excludeFilter in unmanagedSources := {
    excludeTests match {
      case true  => excludePaths((unmanagedSourceDirectories in Test).value.map(dir => dir.getCanonicalPath))
      case false => new SimpleFileFilter(file => false)
    }
  },
  organization := "io.getquill",
  scalaVersion := "2.13.1",
//  crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.1"),
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %%% "scala-collection-compat" % "2.1.6",
    "com.lihaoyi"     %% "pprint"         % pprintVersion(scalaVersion.value),
    "org.scalatest"   %%% "scalatest"     % "3.1.1"          % Test,
    "ch.qos.logback"  % "logback-classic" % "1.2.3"          % Test,
    "com.google.code.findbugs" % "jsr305" % "3.0.2"          % Provided // just to avoid warnings during compilation
  ) ++ {
    if (debugMacro) Seq(
      "org.scala-lang"   %  "scala-library"     % scalaVersion.value,
      "org.scala-lang"   %  "scala-compiler"    % scalaVersion.value,
      "org.scala-lang"   %  "scala-reflect"     % scalaVersion.value
    )
    else Seq()
  },
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignParameters, true)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(FormatXml, true)
    .setPreference(PreserveSpaceBeforeArguments, false)
    .setPreference(DoubleIndentConstructorArguments, false)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(DanglingCloseParenthesis, Force)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentLocalDefs, false)
    .setPreference(SpacesWithinPatternBinders, true)
    .setPreference(SpacesAroundMultiImports, true),
  EclipseKeys.createSrc := EclipseCreateSrc.Default,
  unmanagedClasspath in Test ++= Seq(
    baseDirectory.value / "src" / "test" / "resources"
  ),
  EclipseKeys.eclipseOutput := Some("bin"),
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-Xfatal-warnings",
    "-encoding", "UTF-8",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",

  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) =>
        Seq("-Ypatmat-exhaust-depth", "40")
      case Some((2, 11)) =>
        Seq("-Xlint",
          "-Xfatal-warnings",
          "-Xfuture",
          "-deprecation",
          "-Yno-adapted-args",
          "-Ywarn-unused-import", "" +
          "-Xsource:2.12" // needed so existential types work correctly
        )
      case Some((2, 12)) =>
        Seq("-Xlint:-unused,_",

          "-Xfuture",
          "-deprecation",
          "-Yno-adapted-args",
          "-Ywarn-unused:imports",
          "-Ycache-macro-class-loader:last-modified"
        )
      case _ => Seq()
    }
  },
  concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
  scoverage.ScoverageKeys.coverageMinimum := 96,
  scoverage.ScoverageKeys.coverageFailOnMinimum := false
)

def doOnDefault(steps: ReleaseStep*): Seq[ReleaseStep] =
  Seq[ReleaseStep](steps: _*)

def doOnPush(steps: ReleaseStep*): Seq[ReleaseStep] =
  if (skipPush)
    Seq[ReleaseStep]()
  else
    Seq[ReleaseStep](steps: _*)

lazy val commonSettings = ReleasePlugin.extraReleaseCommands ++ basicSettings ++ Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pgpSecretRing := file("local.secring.gpg"),
  pgpPublicRing := file("local.pubring.gpg"),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) =>
        doOnDefault(checkSnapshotDependencies) ++
        doOnDefault(inquireVersions) ++
        doOnDefault(runClean) ++
        doOnPush   (setReleaseVersion) ++
        doOnDefault(updateReadmeVersion(_._1)) ++
        doOnPush   (commitReleaseVersion) ++
        doOnPush   (updateWebsiteTag) ++
        doOnPush   (tagRelease) ++
        doOnDefault(publishArtifacts) ++
        doOnPush   (setNextVersion) ++
        doOnPush   (updateReadmeVersion(_._2)) ++
        doOnPush   (commitNextVersion) ++
        //doOnPush(releaseStepCommand("sonatypeReleaseAll")) ++
        doOnPush   (pushChanges)
      case Some((2, 12)) =>
        doOnDefault(checkSnapshotDependencies) ++
        doOnDefault(inquireVersions) ++
        doOnDefault(runClean) ++
        doOnPush   (setReleaseVersion) ++
        doOnDefault(publishArtifacts)
        //doOnPush   ("sonatypeReleaseAll") ++
      case _ => Seq[ReleaseStep]()
    }
  },
  pomExtra := (
    <url>http://github.com/getquill/quill</url>
    <licenses>
      <license>
        <name>Apache License 2.0</name>
        <url>https://raw.githubusercontent.com/getquill/quill/master/LICENSE.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:getquill/quill.git</url>
      <connection>scm:git:git@github.com:getquill/quill.git</connection>
    </scm>
    <developers>
      <developer>
        <id>fwbrasil</id>
        <name>Flavio W. Brasil</name>
        <url>http://github.com/fwbrasil/</url>
      </developer>
    </developers>)
)
