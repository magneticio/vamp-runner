package io.vamp.runner

import java.util.UUID

import io.vamp.runner.Recipe.Method.MethodType
import io.vamp.runner.Recipe.State
import io.vamp.runner.Recipe.State.StateType

object Recipe {

  object State extends Enumeration {
    type StateType = Value

    val Idle, Success, Failure, Running, Aborted = Value
  }

  object Method extends Enumeration {
    type MethodType = Value

    val POST, PUT, DELETE = Value
  }

  def apply(name: String, description: String, steps: List[RecipeStep]): Recipe = {
    Recipe(UUID.randomUUID().toString, name, description, steps)
  }
}

case class Recipe(id: String, name: String, description: String, steps: List[RecipeStep])

case class RecipeStep(description: String, execute: RecipeStepAction, cleanup: RecipeStepAction, state: StateType = State.Idle)

case class RecipeStepAction(method: MethodType, resource: String, await: Set[String])
