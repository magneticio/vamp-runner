package io.vamp.runner

import java.util.UUID

import io.vamp.runner.Recipe.State.StateType

object Recipe {

  object State extends Enumeration {
    type StateType = Value

    val Idle, Success, Failure, Running, Aborted = Value
  }

  def apply(name: String, description: String): Recipe = Recipe(UUID.randomUUID().toString, name, description, Recipe.State.Idle)
}

case class Recipe(id: String, name: String, description: String, state: StateType)
