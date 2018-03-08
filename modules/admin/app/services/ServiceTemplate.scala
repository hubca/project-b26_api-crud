package services

import scala.language.implicitConversions

object ServiceTemplate {

  class RichString(string: String) {
    def isActive = """class="active""""
    /*
    def isActive(predicate: => Boolean) = string match {
      case true => """class = "active""""
      case _ => ""
    }
    */
  }

  //implicit def richString(active: Boolean): RichString = new RichString(active)
  implicit def richString(string: String): RichString = new RichString(string)
}
