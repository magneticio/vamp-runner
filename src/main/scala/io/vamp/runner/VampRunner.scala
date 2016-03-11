package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.Logger
import io.vamp.runner.recipe.Recipe
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.language.postfixOps

object VampRunner extends App with VampRecipes {

  val logger = Logger(LoggerFactory.getLogger(VampRunner.getClass))

  logger.info(
    s"""
       |██╗   ██╗ █████╗ ███╗   ███╗██████╗
       |██║   ██║██╔══██╗████╗ ████║██╔══██╗
       |██║   ██║███████║██╔████╔██║██████╔╝
       |╚██╗ ██╔╝██╔══██║██║╚██╔╝██║██╔═══╝
       | ╚████╔╝ ██║  ██║██║ ╚═╝ ██║██║
       |  ╚═══╝  ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝
       |                       runner
       |                       by magnetic.io
    """.stripMargin)

  logger.info(s"Vamp API URL: ${Vamp.apiUrl}")

  implicit val actorSystem = ActorSystem("vamp-runner")
  implicit val executionContext = actorSystem.dispatcher

  Http(actorSystem)

  val runnables: List[(String, Recipe)] = if (args.isEmpty) recipes
  else args.map {
    case name ⇒ recipes.find(_._1 == name).getOrElse({
      throw new RuntimeException(s"No recipe: $name")
    })
  } toList

  logger.info("Running recipes...")

  runnables.foldLeft(Future.successful[Any]({}))({
    case (f, (name, recipe)) ⇒
      f flatMap { _ ⇒
        logger.info(s"Running recipe: $name")
        recipe.run
      }
  }) recover {
    case failure ⇒ logger.error(s"Failure: ${failure.getMessage}")
  } onComplete {
    case _ ⇒
      logger.info("Done.")
      Http().shutdownAllConnectionPools()
      actorSystem.terminate()
  }
}
