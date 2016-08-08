package io.vamp.runner

import java.util.UUID

import io.vamp.runner.Recipe.State.StateType

object Recipe {

  object State extends Enumeration {
    type StateType = Value

    val Idle, Succeeded, Failed, Running = Value
  }

}

case class Recipe(id: String = UUID.randomUUID().toString, name: String, description: String, run: List[RunRecipeStep], cleanup: List[CleanupRecipeStep])

sealed trait RecipeStep {

  def id: String

  def description: String

  def resource: String

  def await: Set[String]
}

case class RunRecipeStep(
  id: String = UUID.randomUUID().toString,
  description: String,
  resource: String,
  await: Set[String],
  dirty: Boolean = false,
  state: StateType = Recipe.State.Idle) extends RecipeStep

case class CleanupRecipeStep(
  id: String = UUID.randomUUID().toString,
  description: String,
  resource: String,
  await: Set[String]) extends RecipeStep
