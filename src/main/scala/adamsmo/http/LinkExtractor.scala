package adamsmo.http

import adamsmo.conf.Configuration
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.Materializer
import akka.util.ByteString
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LinkExtractor(conf: Configuration)(implicit mat: Materializer) {
  def extractLinks(httpResponse: Future[HttpResponse]): Future[ExtractedLinks] = httpResponse.flatMap {
    case HttpResponse(StatusCodes.OK | StatusCodes.Found, _, entity, _) =>

      entity.dataBytes
        .scan((ByteString.empty, 0)) { case ((_, size), next) => (next, size + next.length) }
        .takeWhile { case (_, size) => size < conf.maxPageSize }
        .map { case (chunk, _) => chunk }
        .runFold(ByteString.empty)((prev, next) => prev ++ next)
        .map(_.utf8String)
        .map { page =>
          val doc = Jsoup.parse(page)
          val links = doc.select("a[href]").iterator().asScala.toSet[Element]
          val absoluteLinks = links.map(e => e.attr("abs:href")).filter(_.nonEmpty)
          val relativeLinks = links.map(e => e.attr("href")) -- absoluteLinks

          ExtractedLinks(absoluteLinks, relativeLinks)
        }

    case HttpResponse(code, _, _, _) => Future.failed(new RuntimeException(s"wrong response code, expected 200 or 302 got $code"))
  }
}
