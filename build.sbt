name := "imagePoll"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.reactivemongo" %% "reactivemongo" % "0.10.0",
  "com.github.nscala-time" %% "nscala-time" % "0.8.0"
)     

play.Project.playScalaSettings
