package io.vamp.runner

import java.util.UUID

import io.vamp.runner.Recipe.Method.MethodType
import io.vamp.runner.Recipe.State
import io.vamp.runner.Recipe.State.StateType

object Recipe {

  object State extends Enumeration {
    type StateType = Value

    val Idle, Succeeded, Failed, Running, Aborted = Value
  }

  object Method extends Enumeration {
    type MethodType = Value

    val POST, PUT, DELETE = Value
  }
}

case class Recipe(id: String = UUID.randomUUID().toString, name: String, description: String, steps: List[RecipeStep])

case class RecipeStep(id: String = UUID.randomUUID().toString, description: String, execute: RecipeStepAction, cleanup: RecipeStepAction) {
  val state: StateType = if (Math.random() > 0.2) State.Succeeded else State.Failed
}

case class RecipeStepAction(method: MethodType, resource: String, await: Set[String])
