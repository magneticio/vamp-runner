package io.vamp.runner

import io.vamp.runner.Recipe.State.StateType

object Recipe {

  object State extends Enumeration {
    type StateType = Value

    val Idle, Success, Failure, Running, Aborted = Value
  }

}

case class Recipe(id: String, title: String, state: StateType)
