name := """Veritask"""

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

pipelineStages := Seq(uglify, digest, gzip)

pipelineStages in Assets := Seq()

pipelineStages := Seq(uglify, digest, gzip)

routesGenerator := InjectedRoutesGenerator

DigestKeys.algorithms += "sha1"

UglifyKeys.uglifyOps := { js =>
  Seq((js.sortBy(_._2), "concat.min.js"))
}

resolvers += "bblfish-snapshots" at "http://bblfish.net/work/repo/snapshots/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "4.0",
  "javax.inject" % "javax.inject" % "1",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play24",
  "org.reactivemongo" % "reactivemongo-extensions-json_2.11" % "0.11.7.play24",
  "com.adrianhurt" %% "play-bootstrap" % "1.0-P24-B3-SNAPSHOT",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

libraryDependencies += filters
libraryDependencies += specs2 % Test
libraryDependencies += "org.w3" %% "banana-sesame" % "0.8.2-SNAPSHOT" //"0.8.1"

//updateOptions := updateOptions.value.withCachedResolution(true)

//fork in run := true