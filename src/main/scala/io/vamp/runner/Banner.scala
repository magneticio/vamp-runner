package io.vamp.runner

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.sys.process._
import scala.util.Try

trait Banner {

  def banner() = {

    val logger = Logger(LoggerFactory.getLogger(VampRunner.getClass))

    logger.info(
      s"""
       |██╗   ██╗ █████╗ ███╗   ███╗██████╗     ██████╗ ██╗   ██╗███╗   ██╗███╗   ██╗███████╗██████╗
       |██║   ██║██╔══██╗████╗ ████║██╔══██╗    ██╔══██╗██║   ██║████╗  ██║████╗  ██║██╔════╝██╔══██╗
       |██║   ██║███████║██╔████╔██║██████╔╝    ██████╔╝██║   ██║██╔██╗ ██║██╔██╗ ██║█████╗  ██████╔╝
       |╚██╗ ██╔╝██╔══██║██║╚██╔╝██║██╔═══╝     ██╔══██╗██║   ██║██║╚██╗██║██║╚██╗██║██╔══╝  ██╔══██╗
       | ╚████╔╝ ██║  ██║██║ ╚═╝ ██║██║         ██║  ██║╚██████╔╝██║ ╚████║██║ ╚████║███████╗██║  ██║
       |  ╚═══╝  ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝         ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝
       |                                      ${"".padTo(54 - version.length, " ").mkString}v$version""".stripMargin)
  }

  private lazy val version = {
    Option(getClass.getPackage.getImplementationVersion).orElse {
      Try(Option("git describe --tags".!!.stripLineEnd)).getOrElse(None)
    } getOrElse ""
  }
}
