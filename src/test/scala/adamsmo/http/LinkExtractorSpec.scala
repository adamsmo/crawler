package adamsmo.http

import java.nio.file.Paths

import adamsmo.conf.Configuration
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

class LinkExtractorSpec extends FlatSpec with Matchers with ScalaFutures with PatienceConfiguration {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(50, Millis))

  "LinkExtractorSpec" should "extract links from correct html" in new Context {
    val pageWithLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithLinks, 4)))

    service.getLinks(pageWithLinksResponse).futureValue shouldEqual ExtractedLinks(Set("http://pl.pl", "https://pll.pl"), Set("/profile.html"))
  }

  it should "return empty link List if there are no links in html" in new Context {
    val pageWithoutLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithoutLink, 4)))

    service.getLinks(pageWithoutLinksResponse).futureValue shouldEqual ExtractedLinks(Set.empty, Set.empty)
  }

  it should "return no links if response is not html" in new Context {
    val notHtml: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, ByteString(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)))))

    service.getLinks(notHtml).futureValue shouldEqual ExtractedLinks(Set.empty, Set.empty)
  }

  it should "return failure response future failed" in new Context {
    val exception = new RuntimeException
    val failedResponse: Future[HttpResponse] = Future.failed(exception)

    service.getLinks(failedResponse).failed.futureValue shouldEqual exception
  }

  it should "return no links if they were clipped because site is too big" in new Context {
    val strictConf = new Configuration {
      override def maxPageSize: Int = 5
    }

    val pageWithLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithLinks, 4)))

    new LinkExtractor(strictConf).getLinks(pageWithLinksResponse).futureValue shouldEqual ExtractedLinks(Set.empty, Set.empty)
  }

  it should "return links if they were not clipped on too big site" in new Context {
    val strictConf = new Configuration {
      override def maxPageSize: Int = 135
    }

    val pageWithLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithLinks, 4)))

    new LinkExtractor(strictConf).getLinks(pageWithLinksResponse).futureValue shouldEqual ExtractedLinks(Set("http://pl.pl"), Set.empty)
  }

  trait Context {
    val pageWithLinks = Paths.get(getClass.getResource(s"/html_pages/pageWithLink.html").getPath)
    val pageWithoutLink = Paths.get(getClass.getResource(s"/html_pages/pageWithoutLink.html").getPath)

    implicit val actorSystem = ActorSystem("test-system")
    implicit val materializer = ActorMaterializer()

    val conf = new Configuration {
      override def maxPageSize: Int = 5 * 1000
    }

    val service = new LinkExtractor(conf)

  }

}
