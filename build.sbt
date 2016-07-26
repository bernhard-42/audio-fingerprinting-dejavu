name := "AudioFingerprinting"

version := "1.0"

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "commons-io" % "commons-io" % "2.5",
  "nu.pattern" % "opencv" % "2.4.9-7",
  "com.owlike" % "genson-scala_2.10" % "1.4"
)

mainClass in assembly := Some("com.betaocean.audiofingerprint.AudioFingerprint")

// include musicg project
unmanagedSourceDirectories in Compile += baseDirectory.value / "musicg/src"
unmanagedSourceDirectories in Compile += baseDirectory.value / "musicg/graphic"

excludeFilter in unmanagedSources := HiddenFileFilter || "*experiment*" || "*main*" || "*pitch*"

