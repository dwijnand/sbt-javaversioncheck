package com.typesafe.sbt

import xsbti.{ Position, Reporter, Severity }
import xsbti.compile.{ IncToolOptionsUtil, JavaCompiler }
import sbt._
import Keys._

object JavaVersionCheckPlugin extends sbt.AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  object autoImport {
    val javaVersionPrefix = settingKey[Option[String]](
      "java version prefix required by javaVersionCheck")

    val javaVersionCheck = taskKey[String](
      "checks the Java version vs. javaVersionPrefix, returns actual version")
  }
  import autoImport._

  override val projectSettings = javaVersionCheckSettings

  val defaultJavaVersionPrefix: Option[String] = Some("1.7")


  def javaVersionCheckSettings: Seq[Setting[_]] = Seq(
    javaVersionPrefix in javaVersionCheck := defaultJavaVersionPrefix,
    javaVersionCheck := {
      val log = streams.value.log
      val javac = (compileInputs in (Compile, compile)).value.compilers.javaTools.javac
      JavaVersionCheck((javaVersionPrefix in javaVersionCheck).value, javac, log)
    },
    // we hook onto deliverConfiguration to run the version check as early as possible,
    // before we actually do anything. But we don't want to require the version check
    // just for compile.
    deliverConfiguration := {
      val log = streams.value.log
      log.info("will publish with javac version " + javaVersionCheck.value)
      deliverConfiguration.value
    },
    deliverLocalConfiguration := {
      val log = streams.value.log
      log.info("will publish locally with javac version " + javaVersionCheck.value)
      deliverLocalConfiguration.value
    }
  )
}

object JavaVersionCheck {
  def apply(javaVersionPrefix: Option[String], javac: JavaCompiler, realLog: Logger): String = {
    realLog.info(s"JavaCompiler class: ${javac.getClass}")
    val captureVersionLog = new CaptureVersionLogger(realLog: Logger)
    javac.run(
      /* sources = */ Array.empty,
      /* options = */ Array("-version"),
      /* incToolOptions = */ IncToolOptionsUtil.defaultIncToolOptions,
      /* reporter = */ NoopReporter,
      /* log = */ captureVersionLog
    )
    val version: String = captureVersionLog.captured getOrElse
      sys.error("failed to get or parse the output of javac -version")
    javaVersionPrefix match {
      case Some(prefix) =>
        if (!version.startsWith(prefix)) {
          sys.error(
            s"javac version $version may not be used to publish, " +
              s"it has to start with $prefix due to javaVersionPrefix setting")
        }
      case None =>
    }
    version
  }
}

final class CaptureVersionLogger(val realLog: Logger) extends Logger {
  var captured: Option[String] = None
  def log(level: Level.Value, message: => String): Unit = {
    val m = message
    if (level == Level.Warn && m.startsWith("javac ")) {
      captured = Some(m.substring("javac ".length).trim)
    } else {
      realLog.log(level, m)
    }
  }
  def success(message: => String): Unit = realLog.success(message)
  def trace(t: => Throwable): Unit = realLog.trace(t)
}

object NoopReporter extends Reporter {
  def reset() = ()
  def hasErrors = false
  def hasWarnings = false
  def printSummary() = ()
  def problems() = Array.empty
  def log(pos: Position, msg: String, sev: Severity) = ()
  def comment(pos: Position, msg: String) = ()
}
