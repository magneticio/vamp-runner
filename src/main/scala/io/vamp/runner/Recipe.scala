package io.vamp.runner

import java.util.UUID

import io.vamp.runner.Recipe.Method.MethodType
import io.vamp.runner.Recipe.State
import io.vamp.runner.Recipe.State.StateType

object Recipe {

  object State extends Enumeration {
    type StateType = Value

    val Idle, Succeeded, Failed, Running = Value
  }

  object Method extends Enumeration {
    type MethodType = Value

    val POST, PUT, DELETE = Value
  }
}

case class Recipe(id: String = UUID.randomUUID().toString, name: String, description: String, steps: List[RecipeStep])

case class RecipeStep(id: String = UUID.randomUUID().toString, description: String, run: RecipeStepAction, dirty: Boolean = false, cleanup: RecipeStepAction, state: StateType = State.Idle)

case class RecipeStepAction(method: MethodType, resource: String, await: Set[String])
