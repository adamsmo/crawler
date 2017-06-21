package adamsmo.http

import adamsmo.conf.Configuration
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.Materializer
import akka.util.ByteString
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LinkExtractor(conf: Configuration)(implicit mat: Materializer) {

  //todo add timeout on http request
  def getLinks(httpResponse: Future[HttpResponse]): Future[ExtractedLinks] = httpResponse.flatMap {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>

      val pageContent = entity.dataBytes

      pageContent.zip(pageContent.scan(0)((size, chunk) => chunk.length + size).drop(1))
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
  }
}
