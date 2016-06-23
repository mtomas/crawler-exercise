name := "crawler"

organization := "com.dcvc.crawler"

version := "1.2"

scalaVersion := "2.11.7"


libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.4",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.4",
    "org.jsoup" % "jsoup" % "1.8+",
    "commons-validator" % "commons-validator" % "1.5+",
    "org.scalatest"   %% "scalatest"    % "2.2.4"   % "test,it",
    "org.scalacheck"  %% "scalacheck"   % "1.12.5"      % "test,it"
)

// For Settings/Task reference, see http://www.scala-sbt.org/release/sxr/sbt/Keys.scala.html

// Compiler settings. Use scalac -X for other options and their description.
// See Here for more info http://www.scala-lang.org/files/archive/nightly/docs/manual/html/scalac.html 
scalacOptions ++= List("-feature","-deprecation", "-unchecked", "-Xlint")

// ScalaTest settings.
// Ignore tests tagged as @Slow (they should be picked only by integration test)
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-l", "org.scalatest.tags.Slow", "-u","target/junit-xml-reports", "-oD", "-eS")

//Style Check section
scalastyleConfig <<= baseDirectory { _ / "src/main/config" / "scalastyle-config.xml" }

