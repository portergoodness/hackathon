name := "myFirstApp"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "nz.ac.waikato.cms.weka" % "weka-stable" % "3.6.11"
)     

play.Project.playScalaSettings
