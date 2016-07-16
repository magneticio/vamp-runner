package io.vamp.runner

trait Response {

  val `type`: String = this.getClass.getSimpleName.toLowerCase
}