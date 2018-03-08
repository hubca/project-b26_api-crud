package data

import akka.stream.Materializer
import play.api.mvc.{Codec, Result}
import play.twirl.api.Html
import scala.concurrent.{ExecutionContext, Future}

object Pagelet {

  def readBody(result: Result)(implicit mat: Materializer, codec: Codec, exec: ExecutionContext): Future[Html] = {
    result.body.consumeData.map(byteString => Html(codec.decode(byteString)))
  }

}

// todo - test removing Pagelet in main project and using it from here