package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.Logger
import io.vamp.runner.recipe._
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.language.postfixOps

trait VampRecipes {

  implicit def actorSystem: ActorSystem

  lazy val recipes: List[Recipe] = List(
    new VampInfo,
    new VampHttp,
    new VampHttpCanary,
    new VampHttpDependency,
    new VampHttpFlipFlop,
    new VampHttpFlipFlopDependency,
    new VampTcp,
    new VampTcpDependency
  )
}

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

  val runnables: List[Recipe] = if (args.isEmpty) recipes
  else args.map {
    case name ⇒ recipes.find(_.name == name).getOrElse({
      throw new RuntimeException(s"No recipe: $name")
    })
  } toList

  logger.info("Running recipes...")

  var succeeded: List[String] = Nil
  var failed: List[(String, String)] = Nil

  runnables.foldLeft(Future.successful[Any]({}))({
    case (f, recipe) ⇒
      f flatMap { _ ⇒
        logger.info(s"Running recipe: ${recipe.name}")
        recipe.execute map { _ ⇒ succeeded = succeeded :+ recipe.name } recover {
          case failure ⇒
            failed = failed :+ (recipe.name -> failure.getMessage)
            logger.error(s"Failure: ${failure.getMessage}")
        }
      }
  }) onComplete {
    case _ ⇒
      succeeded.foreach { case name ⇒ logger.info(s"Succeeded: $name") }
      failed.foreach { case (name, failure) ⇒ logger.error(s"Failed [$name]: $failure") }
      logger.info("Done.")
      Http().shutdownAllConnectionPools() onComplete { case _ ⇒ actorSystem.terminate() }
  }
}
