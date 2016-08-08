package io.vamp.runner

import java.util.UUID

import io.vamp.runner.Recipe.Method.MethodType
import io.vamp.runner.Recipe.State.StateType
import io.vamp.runner.Recipe.Timeout.TimeoutType

object Recipe {

  object State extends Enumeration {
    type StateType = Value

    val idle, succeeded, failed, running = Value
  }

  object Timeout extends Enumeration {
    type TimeoutType = Value

    val short, long = Value
  }

  object Method extends Enumeration {
    type MethodType = Value

    val create, delete = Value
  }
}

case class Recipe(id: String = UUID.randomUUID().toString, name: String, description: String, run: List[RunRecipeStep], cleanup: List[CleanupRecipeStep])

sealed trait RecipeStep {

  def id: String

  def description: String

  def resource: String

  def await: Set[String]

  def timeout: TimeoutType

  def method: MethodType
}

case class RunRecipeStep(
  id: String = UUID.randomUUID().toString,
  description: String,
  resource: String,
  await: Set[String] = Set(),
  method: MethodType = Recipe.Method.create,
  timeout: TimeoutType = Recipe.Timeout.long,
  dirty: Boolean = false,
  state: StateType = Recipe.State.idle) extends RecipeStep

case class CleanupRecipeStep(
  id: String = UUID.randomUUID().toString,
  description: String,
  resource: String,
  exists: String = "",
  await: Set[String] = Set(),
  method: MethodType = Recipe.Method.delete,
  timeout: TimeoutType = Recipe.Timeout.long) extends RecipeStep
