package io.vamp.runner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.Logger
import io.vamp.runner.recipe._
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }

object VampRunner extends App with Runner {

  implicit val actorSystem = ActorSystem("vamp-runner")
  implicit val executionContext = actorSystem.dispatcher

  val availableArguments = List(
    CommandLineArgument("h", "help", "Print this help."),
    CommandLineArgument("l", "list", "List all recipes."),
    CommandLineArgument("a", "all", "Run all recipes."),
    CommandLineArgument("r", "run", "Run named recipe(s).")
  )

  logger.info(logo)

  logger.info(s"Vamp API URL     : ${Vamp.apiUrl}")
  logger.info(s"Vamp Gateway Host: ${Vamp.vgaHost}")

  parse(args)

  def help() = {
    logger.info("Usage:")
    availableArguments.foreach(arg ⇒ logger.info(arg.toString))
  }

  if (hasArgument("help")) help()

  if (hasArgument("list")) recipes.foreach(recipe ⇒ logger.info(s"${recipe.name.padTo(30, " ").mkString} - ${recipe.description}"))

  if (hasArgument("all"))
    execute()
  else (hasArgument("run"), getValues("run")) match {
    case (true, run) if run.nonEmpty ⇒ execute(run)
    case _ ⇒
      if (args.isEmpty) help()
      shutdown()
  }
}

trait Runner extends VampRecipes with CommandLineParser {

  val logger = Logger(LoggerFactory.getLogger(VampRunner.getClass))

  implicit def actorSystem: ActorSystem

  implicit def executionContext: ExecutionContext

  def logo = {
    s"""
       |██╗   ██╗ █████╗ ███╗   ███╗██████╗
       |██║   ██║██╔══██╗████╗ ████║██╔══██╗
       |██║   ██║███████║██╔████╔██║██████╔╝
       |╚██╗ ██╔╝██╔══██║██║╚██╔╝██║██╔═══╝
       | ╚████╔╝ ██║  ██║██║ ╚═╝ ██║██║
       |  ╚═══╝  ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝
       |                       runner ${Vamp.version}
       |                       by magnetic.io
    """.stripMargin
  }

  def execute(args: List[String] = Nil) = {
    val runnables: List[Recipe] = if (args.isEmpty) recipes
    else args.map {
      case name ⇒ recipes.find(_.name == name).getOrElse({
        throw new RuntimeException(s"No recipe: $name")
      })
    }

    logger.info("Running recipes...")

    var succeeded: List[String] = Nil
    var failed: List[(String, String)] = Nil

    runnables.foldLeft(Future.successful[Any]({}))({
      case (f, recipe) ⇒
        f flatMap { _ ⇒
          logger.info(s"Recipe name       : ${recipe.name}")
          logger.info(s"Recipe description: ${recipe.description}")
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
        shutdown()
    }
  }

  def shutdown() = Http().shutdownAllConnectionPools() onComplete { case _ ⇒ actorSystem.terminate() }
}