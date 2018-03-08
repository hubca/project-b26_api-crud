package uit

import akka.NotUsed
import akka.stream.scaladsl.Source
import play.twirl.api._
import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

/**
  * Created by sambo on 14/08/2017.
  */

case class HtmlStream1(source: Source[Html, NotUsed]) extends Appendable[HtmlStream1] {

  def +=(other: HtmlStream1): HtmlStream1 = andThen(other)
  def andThen(other: HtmlStream1): HtmlStream1 = HtmlStream1(source.merge(other.source))

}

object HtmlStream1 {

  def apply(text: String): HtmlStream1 = apply(Html(text))
  def apply(html: Html): HtmlStream1 = HtmlStream1(Source.single(html))

}

object HtmlStreamFormat1 extends Format[HtmlStream1] {
  def raw(text: String): HtmlStream1 = HtmlStream1(text)
  def escape(text: String): HtmlStream1 = raw(HtmlFormat.escape(text).body)

  override def empty: HtmlStream1 = raw("")
  override def fill(elements: immutable.Seq[HtmlStream1]): HtmlStream1 = elements.reduce((agg, curr) => agg.andThen(curr))
}

object HtmlStreamImplicits1 {
  // Implicit conversion so HtmlStream1 can be passed directly to Ok.feed and Ok.chunked
  implicit def toSource(stream: HtmlStream1)(implicit ec: ExecutionContext): Source[Html, NotUsed] = {
    stream.source.filter(_.body.nonEmpty)
  }

}