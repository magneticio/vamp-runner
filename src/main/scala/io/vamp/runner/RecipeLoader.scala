package io.vamp.runner

import akka.actor.ActorLogging
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._

import scala.io.Source

trait RecipeLoader {
  this: ActorLogging ⇒

  private implicit val formats = DefaultFormats

  private val files = Config.stringList("vamp.runner.recipes.files")

  protected def load: List[Recipe] = {
    files.map { file ⇒

      log.info(s"Loading recipe: $file")

      val map = read[Any](Source.fromFile(file).mkString).asInstanceOf[Map[String, Any]]

      Recipe(
        name = map.getOrElse("name", {
          throw new RuntimeException("No recipe name.")
        }).toString,
        description = map.getOrElse("description", {
          throw new RuntimeException("No recipe description.")
        }).toString
      )
    }
  }
}
