package controllers.admin

import javax.inject.Inject
import play.api.mvc.{BaseController, ControllerComponents}

class HomeController6 @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def index = Action { implicit request =>
    Ok("delete")
    // Ok("admin numero 6")
  }

}