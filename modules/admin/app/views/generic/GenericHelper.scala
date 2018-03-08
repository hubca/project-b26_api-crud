package views.generic

object GenericHelper {

  def isActive(idx: Int, activeIdx: Int, suffix: String = ""): String = idx match {
    case idx if ((activeIdx - 1) == idx) => "active" + suffix
    case _ => ""
  }

  def getCollapseAttributes(collapseId: String):String = {
    if(collapseId!="") { s" data-toggle=collapse data-target=#$collapseId" }
    else { "" }
  }

}