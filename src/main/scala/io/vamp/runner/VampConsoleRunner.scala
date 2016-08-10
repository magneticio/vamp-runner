package io.vamp.runner

import akka.actor.{ Actor, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import io.vamp.runner.Hub.Broadcast
import io.vamp.runner.RunnerActor.{ ProvideRecipes, Recipes, Run, RunningState }
import org.slf4j.LoggerFactory

import scala.concurrent.{ Future, Promise }

object VampConsoleRunner extends App with CommandLineParser with Banner {

  val logger = Logger(LoggerFactory.getLogger(VampConsoleRunner.getClass))

  implicit val system = ActorSystem("vamp-runner")

  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  implicit val timeout: Timeout = Config.duration("vamp.runner.timeout")

  private val executions = scala.collection.mutable.ArrayBuffer.empty[Future[_]]

  private val runner = system.actorOf(Props(new Actor {

    private lazy val inner = context.actorOf(RunnerActor.props)

    private var promise: Option[Promise[Option[_]]] = None

    def receive = {

      case ProvideRecipes ⇒ inner.forward(ProvideRecipes)

      case (p: Promise[_], r: Run) ⇒
        promise = Option(p.asInstanceOf[Promise[Option[_]]])
        inner.forward(r)

      case Broadcast(RunningState(running)) if !running ⇒
        promise.foreach(_.success(None))
        promise = None

      case _ ⇒
    }
  }))

  banner()

  sys.addShutdownHook {
    shutdown()
  }

  val availableArguments = CommandLineArguments(List(
    CommandLineArgument("h", "help", "Print this help."),
    CommandLineArgument("l", "list", "List all recipes."),
    CommandLineArgument("a", "all", "Run all recipes."),
    CommandLineArgument("r", "run", "Run named recipe(s).")
  ))

  parse(args)

  if (hasArgument("help")) push(Future.successful(availableArguments))

  if (hasArgument("list")) push(runner ? ProvideRecipes)

  if (hasArgument("all") || hasArgument("run")) push {

    val promise = Promise[Option[_]]()

    runner ? ProvideRecipes map {
      case Recipes(recipes) ⇒
        val ids = {
          if (hasArgument("all")) recipes.map(_.id)
          else getValues("run").flatMap(name ⇒ recipes.find(_.name == name)).map(_.id)
        }
        runner ! (promise -> Run(ids))

      case _ ⇒
    }

    promise.future
  }

  collect().onComplete(_ ⇒ shutdown())

  def push: Future[_] ⇒ Unit = future ⇒ executions += future

  def collect() = {

    def line() = logger.info("".padTo(44, "-").mkString)

    def complete: Any ⇒ Unit = {
      case result: Recipes ⇒
        line()
        logger.info("Recipes:")
        result.recipes.foreach(recipe ⇒ logger.info(s"• ${recipe.name.padTo(40, " ").mkString} - ${recipe.description}"))
        line()

      case CommandLineArguments(arguments) ⇒
        line()
        logger.info("Usage:")
        arguments.foreach(arg ⇒ logger.info(arg.toString))
        line()

      case None ⇒

      case other ⇒
        line()
        logger.error(s"Unexpected: ${other.getClass.getSimpleName}")
        line()
    }

    executions.foldLeft[Future[Any]](Future.successful(None))((f, exe) ⇒
      f.recover {
        case failure: Throwable ⇒
          logger.error(failure.getMessage)
          None
      } flatMap { result ⇒
        complete(result)
        exe
      }
    ).map(complete)
  }

  def shutdown() = Http().shutdownAllConnectionPools() onComplete { _ ⇒ system.terminate() }
}
