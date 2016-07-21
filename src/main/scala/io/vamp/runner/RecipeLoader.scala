package io.vamp.runner

import akka.actor.ActorLogging
import org.json4s.native.Serialization._

import scala.io.Source
import scala.language.postfixOps
import scala.util.Try

trait RecipeLoader {
  this: ActorLogging ⇒

  private implicit val formats = Json.formats

  private val files = Config.stringList("vamp.runner.recipes.files")

  protected def load: List[Recipe] = {
    files.flatMap { file ⇒
      log.info(s"Loading recipe: $file")
      Try {
        Option {
          read[Recipe](Source.fromFile(file).mkString)
        }
      } recover {
        case e: Throwable ⇒ log.error(e, e.getMessage); None
      } get
    }
  }
}
