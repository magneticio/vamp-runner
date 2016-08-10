package io.vamp.runner

import scala.language.postfixOps

case class CommandLineArgument(short: String, name: String, description: String) {
  override def toString: String = s"-${short.padTo(5, " ").mkString} --${name.padTo(10, " ").mkString} $description"
}

case class CommandLineArguments(arguments: List[CommandLineArgument])

trait CommandLineParser {

  private var arguments: List[String] = Nil

  private var parsed: List[CommandLineArgument] = Nil

  private var values: Map[String, List[String]] = Map()

  def availableArguments: CommandLineArguments

  def parse(args: Array[String]) = {
    val available = availableArguments
    parsed = args.filter(_.startsWith("-")).map(_.dropWhile(_ == '-').mkString).flatMap { argument ⇒
      available.arguments.find(arg ⇒ arg.short == argument || arg.name == argument)
    } toList

    arguments = args.toList

    var command: Option[String] = None
    values = arguments.groupBy { argument ⇒
      if (argument.startsWith("-")) {
        val cmd = argument.dropWhile(_ == '-').mkString
        command = parsed.find(p ⇒ p.name == cmd || p.short == cmd).map(_.name)
        ""
      } else command.getOrElse("")
    } filter (_._1 != "")
  }

  def hasArgument(argument: String): Boolean = parsed.exists(_.name == argument)

  def getValues(argument: String): List[String] = values.getOrElse(argument, Nil)
}
