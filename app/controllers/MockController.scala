package controllers

import javax.inject._

import akka.actor.{ActorRef, ActorSelection, ActorSystem}
import akka.stream.Materializer
import scala.concurrent.duration._
import play.api.libs.concurrent.Futures._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import play.api.mvc.{Action, Result, _}
import services.ServiceClient


/**
  * Created by sambo on 14/08/2017.
  */
class MockController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext, pbp: PlayBodyParsers) extends AbstractController(cc) {

  def index(serviceName: String) = Action.async { request: Request[AnyContent] =>
    serviceName match {
      case "async1" => respond("11", 0.second)
      case "async2" => respond("24", 3.second)
      case "async3" => respond("asy3", 5.second)
      case "snowfall" => respond("131", 5.second)
      case "boardingArea" => respond("17.2", 1.second)
      case "stream" => stream("stream me", 500.millisecond)
      case "stream2" => stream("feed you", 2000.millisecond)
    }
  }

  private def respond(data: String, delay: FiniteDuration): Future[Result] = {
    val promise: Promise[Result] = Promise[Result]()
    actorSystem.scheduler.scheduleOnce(delay) { promise.success(Ok(data)) }
    promise.future
  }

  private def stream(data: String, delay: FiniteDuration): Future[Result] = {
    akka.pattern.after(delay, actorSystem.scheduler){Future.successful(Ok(data))}
  }

}
