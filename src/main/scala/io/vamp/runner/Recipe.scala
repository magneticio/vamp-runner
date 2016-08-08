package io.vamp.runner

import java.util.UUID

import io.vamp.runner.Recipe.State.StateType

object Recipe {

  object State extends Enumeration {
    type StateType = Value

    val idle, succeeded, failed, running = Value
  }

  object Timeout extends Enumeration {
    type TimeoutType = Value

    val short, long = Value
  }
}

case class Recipe(id: String = UUID.randomUUID().toString, name: String, description: String, run: List[RunRecipeStep], cleanup: List[CleanupRecipeStep])

sealed trait RecipeStep {

  def id: String

  def description: String

  def resource: String

  def await: Set[String]

  def timeout: Recipe.Timeout.Value
}

case class RunRecipeStep(
  id: String = UUID.randomUUID().toString,
  description: String,
  resource: String,
  await: Set[String],
  timeout: Recipe.Timeout.Value,
  dirty: Boolean = false,
  state: StateType = Recipe.State.idle) extends RecipeStep

case class CleanupRecipeStep(
  id: String = UUID.randomUUID().toString,
  description: String,
  resource: String,
  exists: String,
  await: Set[String],
  timeout: Recipe.Timeout.Value) extends RecipeStep
