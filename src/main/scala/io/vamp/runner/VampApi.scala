package io.vamp.runner

import com.typesafe.config.ConfigFactory

object VampApi {

  private val config = ConfigFactory.load().getConfig("vamp.runner")

  val host = config.getString("host")
  val port = config.getInt("port")
  val api = config.getString("api")

  val url = s"http://$host${if (port == 80) "" else s":$port"}$api"
}
