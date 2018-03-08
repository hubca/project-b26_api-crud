package views

object ViewHelper {

  def getAllCollections: Seq[String] = List("cty", "our", "rgn", "rst", "scr") // ** add collections here

  def getSidebarNavigationContent: Map[String, Seq[String]] = {
    Map("dbCollectionNames" -> getAllCollections)
  }

  def getCollapseAttributes(collapseId: String):String = {
    if(collapseId!="") { s" data-toggle=collapse data-target=#$collapseId" }
    else { "" }
  }

}