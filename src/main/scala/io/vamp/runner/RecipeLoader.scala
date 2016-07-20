package io.vamp.runner

import akka.actor.ActorLogging
import org.json4s.JsonAST.JString
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonParser._
import org.json4s.{ DefaultFormats, Extraction }

import scala.io.Source
import scala.language.postfixOps
import scala.util.Try

trait RecipeLoader {
  this: ActorLogging ⇒

  private implicit val formats = DefaultFormats +
    new EnumNameSerializer(Recipe.Method) +
    new EnumNameSerializer(Recipe.State)

  private val files = Config.stringList("vamp.runner.recipes.files")

  protected def load: List[Recipe] = {
    files.flatMap { file ⇒
      log.info(s"Loading recipe: $file")
      Try {
        Option {

          val json = parse(Source.fromFile(file).mkString)

          val name = json \ "name" match {
            case JString(s) ⇒ s
            case _          ⇒ throw new RuntimeException("No recipe name.")
          }

          val description = json \ "description" match {
            case JString(s) ⇒ s
            case _          ⇒ throw new RuntimeException("No recipe description.")
          }

          val steps = Extraction.extract[List[RecipeStep]](json \ "steps")

          Recipe(name, description, steps)
        }
      } recover {
        case e: Throwable ⇒ log.error(e, e.getMessage); None
      } get
    }
  }
}
