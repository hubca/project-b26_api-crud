package services

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws._

/* Created by bosis on 14/08/2017 */
class ServiceClient @Inject() (ws: WSClient)(implicit exec: ExecutionContext) {

  def makeCall(url: String) = ws.url(url).get()

  def makeServiceCall(serviceName: String): Future[String] = makeCall(s"http://localhost:9000/mock/$serviceName").map(_.body)

}
