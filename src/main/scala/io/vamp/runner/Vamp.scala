package io.vamp.runner

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.sys.process._
import scala.util.Try

object Vamp {

  private val config = ConfigFactory.load().getConfig("vamp.runner")

  val apiUrl = config.getString("api-url")

  val timeout = config.getInt("timeout") seconds

  val vgaHost = config.getString("vamp-gateway-agent-host")

  val version: String = Option(getClass.getPackage.getImplementationVersion).orElse {
    Try(Option("git describe --tags".!!.stripLineEnd)).getOrElse(None)
  } getOrElse ""
}
