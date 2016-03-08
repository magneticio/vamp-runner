package io.vamp.runner

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object VampApi {

  private val config = ConfigFactory.load().getConfig("vamp.runner")

  val url = config.getString("url")

  val timeout = config.getInt("timeout") seconds
}
