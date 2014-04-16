name := "ipic"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.jasypt" % "jasypt" % "1.9.2",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.50",
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.cloudphysics" % "jerkson_2.10" % "0.6.3",
  "com.amazonaws" % "aws-java-sdk" % "1.7.5"
)     

play.Project.playScalaSettings
