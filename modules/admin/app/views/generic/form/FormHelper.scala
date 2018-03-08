package views.generic.form

object FormHelper {

  def camelize(text: String): String = {

    def _camelize(oldLetterList: List[String], capitalisedText: String = "", thisCamelCaseBool: Boolean = false): String = {

      oldLetterList match {

        case Nil => capitalisedText.trim.replaceAll(" ", "")
        case x :: xs =>

          val thisX = thisCamelCaseBool match {
            case true => x.toUpperCase
            case false => x
          }

          val nextCamelCaseBool = thisX match {
            case " " => true
            case _ => false
          }

          _camelize(xs, capitalisedText + thisX, nextCamelCaseBool)
      }
    }

    val letterList = text.toList.map(x => x.toString)
    _camelize(letterList)

  }

  /*
  def getPlace(text: String): String = {
    "fds"
  }
  */

  def getPlaceholder(text: String): String = {

    text match {
      case text if text.contains("date") => "e.g. 2017-11-22"
      case text if text.contains("bson") => "e.g. 59ea36d3fdc2ce3d73c4be94"
      case _ => ""
    }
  }

}