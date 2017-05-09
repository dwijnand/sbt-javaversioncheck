organization := "com.typesafe.sbt"
        name := "sbt-javaversioncheck"
     version := "0.1.0"
    licenses := Seq("MIT License" -> url("http://opensource.org/licenses/MIT"))
 description := "sbt plugin to check Java version"
  developers := List(Developer("havocp", "Havoc Pennington", "hp pobox com", url("http://ometer.com")))
   startYear := Some(2014)
    homepage := scmInfo.value map (_.browseUrl)
     scmInfo := Some(ScmInfo(url("https://github.com/sbt/sbt-javaversioncheck"), "scm:git:git@github.com:sbt/sbt-javaversioncheck.git"))

sbtPlugin := true

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
