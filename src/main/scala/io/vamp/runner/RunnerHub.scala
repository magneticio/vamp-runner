package io.vamp.runner

import akka.actor.{ ActorRef, ActorSystem }

class RunnerHub(implicit val system: ActorSystem) extends MessageHub {

  protected def onReceive(sender: ActorRef) = {
    case _ ⇒
  }
}
