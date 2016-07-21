package io.vamp.runner

import akka.actor.ActorLogging
import org.json4s.native.Serialization._

import scala.io.Source
import scala.language.postfixOps
import scala.reflect.io.File
import scala.util.Try

trait RecipeLoader {
  this: ActorLogging ⇒

  private implicit val formats = Json.format

  private val files = Config.stringList("vamp.runner.recipes.files")

  protected def load: List[Recipe] = {
    files.flatMap { file ⇒

      log.info(s"Loading recipe: $file")

      def load(relative: String): String = {
        Source.fromFile(s"${File(file).parent.toString}${File.separator}$relative").mkString
      }

      val recipe = Try {
        Option {
          read[Recipe](Source.fromFile(file).mkString)
        }
      } recover {
        case e: Throwable ⇒ log.error(e, e.getMessage); None
      } get

      recipe map { recipe ⇒
        recipe.copy(steps = recipe.steps.map { step ⇒
          step.copy(
            run = step.run.copy(resource = load(step.run.resource)),
            cleanup = step.cleanup.copy(resource = load(step.cleanup.resource))
          )
        })
      }
    }
  }
}
