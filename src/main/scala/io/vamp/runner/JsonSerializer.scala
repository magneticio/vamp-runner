package io.vamp.runner

import org.json4s.ext.EnumNameSerializer
import org.json4s.{ DefaultFormats, FieldSerializer }

trait JsonSerializer {

  implicit private val formats = new DefaultFormats {
    override val fieldSerializers: List[(Class[_], FieldSerializer[_])] = (classOf[AnyRef], new FieldSerializer[AnyRef](
      {
        case (name, value) ⇒ Option((underscore(name), value))
      }
    )) :: Nil
  } + new EnumNameSerializer(Recipe.State)

  def write(any: AnyRef): String = org.json4s.native.Serialization.write(any)

  private def underscore(s: String): String = {
    var lower = false
    val snake = new StringBuilder

    for (c ← s.toCharArray) {
      val previous = lower
      lower = !Character.isUpperCase(c)
      if (previous && !lower) snake.append("_")
      snake.append(c)
    }

    snake.toString().toLowerCase
  }
}
