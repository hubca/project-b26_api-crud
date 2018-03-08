package ui

import akka.NotUsed
import akka.stream.scaladsl.Source
import play.twirl.api._
import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

case class HtmlStream(source: Source[Html, NotUsed]) extends Appendable[HtmlStream] {

  def +=(other: HtmlStream): HtmlStream = andThen(other)
  def andThen(other: HtmlStream): HtmlStream = HtmlStream(source.merge(other.source))

}

object HtmlStream {

  def apply(text: String): HtmlStream = apply(Html(text))
  def apply(html: Html): HtmlStream = HtmlStream(Source.single(html))

}

object HtmlStreamFormat extends Format[HtmlStream] {
  def raw(text: String): HtmlStream = HtmlStream(text)
  def escape(text: String): HtmlStream = raw(HtmlFormat.escape(text).body)

  override def empty: HtmlStream = raw("")
  override def fill(elements: immutable.Seq[HtmlStream]): HtmlStream = elements.reduce((agg, curr) => agg.andThen(curr))
}

object HtmlStreamImplicits {
  // Implicit conversion so HtmlStream can be passed directly to Ok.feed and Ok.chunked
  implicit def toSource(stream: HtmlStream)(implicit ec: ExecutionContext): Source[Html, NotUsed] = {
    stream.source.filter(_.body.nonEmpty)
  }

}