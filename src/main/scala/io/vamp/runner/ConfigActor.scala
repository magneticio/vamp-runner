package io.vamp.runner

import akka.actor.{ Actor, ActorLogging, Props }

import scala.language.postfixOps

object ConfigActor {

  def props: Props = Props(classOf[ConfigActor])

  object ProvideConfig

  case class Config(timeout: Long) extends Response

}

class ConfigActor extends Actor with ActorLogging {

  def receive: Receive = {
    case ConfigActor.ProvideConfig ⇒ sender() ! ConfigActor.Config(Config.duration("vamp.runner.recipes.timeout").toSeconds)
    case _                         ⇒
  }
}
