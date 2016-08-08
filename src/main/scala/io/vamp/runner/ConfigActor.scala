package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, Props }
import io.vamp.runner.ConfigActor.TimeoutConfig

object ConfigActor {

  def props: Props = Props(classOf[ConfigActor])

  object ProvideConfig

  case class Config(timeout: TimeoutConfig) extends Response

  case class TimeoutConfig(short: Long, long: Long)
}

class ConfigActor extends Actor with ActorLogging {

  def receive: Receive = {
    case ConfigActor.ProvideConfig ⇒ sender() ! ConfigActor.Config(
      TimeoutConfig(
        Config.duration("vamp.runner.recipes.timeout.short").toSeconds,
        Config.duration("vamp.runner.recipes.timeout.long").toSeconds
      )
    )
    case _ ⇒
  }
}
