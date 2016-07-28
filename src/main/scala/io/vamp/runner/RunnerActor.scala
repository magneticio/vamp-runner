package io.vamp.runner

import java.util.concurrent.LinkedBlockingQueue

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.agent.Agent
import akka.stream.ActorMaterializer
import io.vamp.runner.Hub.Broadcast

import scala.collection.mutable

object RunnerActor {

  def props(implicit materializer: ActorMaterializer): Props = Props(classOf[RunnerActor], materializer)

  object ProvideRecipes

  case class Run(arguments: AnyRef)

  case class Cleanup(arguments: AnyRef)

  case class Recipes(recipes: List[Recipe]) extends Response

  case class UpdateState(recipe: Recipe, step: RecipeStep, state: Recipe.State.Value)

  case class UpdateDirtyFlag(recipe: Recipe, step: RecipeStep, dirty: Boolean)

}

class RunnerActor(implicit val materializer: ActorMaterializer)
    extends Actor
    with ActorLogging
    with RecipeLoader
    with RecipeRunner
    with VampEventReader {

  import RunnerActor._
  import VampEventReader._

  val timeout = Config.duration("vamp.runner.timeout")

  implicit def system: ActorSystem = context.system

  private val recipes = mutable.LinkedHashMap(load.map(recipe ⇒ recipe.id -> recipe): _*)

  private val running = Agent[Boolean](false)

  protected val events = new LinkedBlockingQueue[VampEventMessage](30)

  def receive: Receive = {
    case ProvideRecipes                       ⇒ sender() ! Recipes(recipes.values.toList)
    case Run(arguments)                       ⇒ if (!running.get()) run()(arguments)
    case Cleanup(arguments)                   ⇒ if (!running.get()) cleanup()(arguments)
    case UpdateState(recipe, step, state)     ⇒ update(recipe, step, state)
    case UpdateDirtyFlag(recipe, step, dirty) ⇒ update(recipe, step, dirty)
    case message: VampEventMessage            ⇒ event(message)
    case _                                    ⇒
  }

  override def preStart(): Unit = sse()

  private def run(): PartialFunction[AnyRef, Unit] = {
    case list: List[_] ⇒
      startRun()
      run(ids2recipes(list).map { recipe ⇒
        recipes += (recipe.id -> recipe.copy(steps = recipe.steps.map { step ⇒ step.copy(state = Recipe.State.Idle) }))
        recipe
      }).onComplete(_ ⇒ endRun())

    case map: Map[_, _] ⇒ for {
      recipe ← map.asInstanceOf[Map[String, _]].get("recipe").flatMap(id ⇒ recipes.get(id.toString))
      step ← map.asInstanceOf[Map[String, _]].get("step").flatMap(id ⇒ recipe.steps.find(_.id == id))
    } yield {
      startRun()
      run(recipe, step).onComplete(_ ⇒ endRun())
    }
  }

  private def cleanup(): PartialFunction[AnyRef, Unit] = {
    case list: List[_] ⇒
      startRun()
      cleanup(ids2recipes(list)).onComplete(_ ⇒ endRun())
  }

  private def update(recipe: Recipe, step: RecipeStep, state: Recipe.State.Value): Unit = {
    update(recipe, step, { step ⇒ step.copy(state = state) })
  }

  private def update(recipe: Recipe, step: RecipeStep, dirty: Boolean): Unit = {
    update(recipe, step, { step ⇒ step.copy(dirty = dirty) })
  }

  private def update(recipe: Recipe, step: RecipeStep, update: RecipeStep ⇒ RecipeStep): Unit = {
    recipes.get(recipe.id).map { r ⇒
      recipes += (r.id -> r.copy(steps = r.steps.map { s ⇒
        if (s.id == step.id) update(s) else s
      }))
    }
    context.parent ! Broadcast(Recipes(RunnerActor.this.recipes.values.toList))
  }

  private def ids2recipes(ids: List[_]): List[Recipe] = ids.flatMap(id ⇒ recipes.get(id.toString))

  private def event(event: VampEventMessage) = {

    event match {
      case e: VampEvent ⇒
        log.info(s"Vamp event: $e")
        context.parent ! Broadcast(e)
      case _ ⇒
    }

    while (!events.offer(event)) events.poll()
  }

  private def startRun() = {
    events.clear()
    running.send(true)
  }

  private def endRun() = {
    self ! VampEventRelease
    running.send(false)
  }
}
