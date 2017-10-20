package controllers

import javax.inject.Inject

import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import scala.concurrent.ExecutionContext.Implicits.global

import models.ResortForm
import services.dbClient._

/**
  * Created by sambo on 18/10/2017.
  */
class UpdateScoresCtr @Inject()(cc: ControllerComponents)(val reactiveMongoApi: ReactiveMongoApi)(adminCtr: AdminCtr, updateScores: UpdateScores) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  override lazy val parse: PlayBodyParsers = cc.parsers

  def updateScore(cat: String) = {
    Action.async { implicit request: Request[AnyContent] =>
      updateScores.updateScore(cat).map(resorts =>
        Ok(views.html.admin.db.rst.index(ResortForm.form, resorts))
      )
    }
  }

}
