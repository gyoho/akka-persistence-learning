name := "demo-akka-persistence-jdbc"

organization := "com.github.dnvriend"

version := "1.0.0"

// the akka-persistence-jdbc plugin lives here
resolvers += Resolver.jcenterRepo

// the slick-extension library (which is used by akka-persistence-jdbc) lives here
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies ++= {
  val akkaVersion = "2.4.7"
  val akkaPersistenceJdbcVersion = "2.4.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
    "com.lihaoyi" %% "pprint" % "0.4.1",
    "org.scalaz" %% "scalaz-core" % "7.2.3",
    "com.twitter" %% "chill-akka" % "0.8.0",
    "com.github.dnvriend" %% "akka-persistence-jdbc" % akkaPersistenceJdbcVersion changing (),
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "org.postgresql" % "postgresql" % "9.4.1208",
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "2.2.6" % Test
  )
}

fork in Test := true

parallelExecution in Test := false

licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import sbtbuildinfo.BuildInfoOption.BuildTime

// Scalariform settings
SbtScalariform.autoImport.scalariformPreferences := SbtScalariform.autoImport.scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
  "scala" -> Apache2_0("2016", "Dennis Vriend"),
  "conf" -> Apache2_0("2016", "Dennis Vriend", "#")
)

// enable sbt-revolver
Revolver.settings ++ Seq(
  Revolver.enableDebugging(port = 5050, suspend = false),
  mainClass in reStart := Some("com.github.dnvriend.Counter")
)

// enable protobuf plugin //
// https://trueaccord.github.io/ScalaPB/sbt-settings.html
import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

PB.protobufSettings

// protoc-jar which is on the sbt classpath //
// https://github.com/os72/protoc-jar
PB.runProtoc in PB.protobufConfig := (args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray))

scalaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(
  _ / "generated-proto")

// build info configuration //
buildInfoKeys := Seq[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion,
  "branch" -> git.gitCurrentBranch,
  "commit" -> git.gitHeadCommit
)

buildInfoPackage := "com.github.dnvriend"
buildInfoOptions += BuildInfoOption.BuildTime
buildInfoOptions += BuildInfoOption.ToJson


// enable plugins
enablePlugins(AutomateHeaderPlugin, BuildInfoPlugin)
