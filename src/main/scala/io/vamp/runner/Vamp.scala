package io.vamp.runner

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object Vamp {

  private val config = ConfigFactory.load().getConfig("vamp.runner")

  val apiUrl = config.getString("api-url")

  val timeout = config.getInt("timeout") seconds

  val vgaHost = config.getString("vamp-gateway-agent-host")
}
