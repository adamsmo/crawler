package adamsmo.http

import java.nio.file.{Path, Paths}

import adamsmo.common.BaseSpec
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.Future

class LinkExtractorSpec extends BaseSpec {

  "LinkExtractor" should "extract links from correct html" in new Context {
    val pageWithLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithLinks, 4)))

    service.extractLinks(pageWithLinksResponse).futureValue shouldEqual ExtractedLinks(Set("http://pl.pl", "https://pll.pl"), Set("/profile.html"))
  }

  it should "return empty link List if there are no links in html" in new Context {
    val pageWithoutLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithoutLink, 4)))

    service.extractLinks(pageWithoutLinksResponse).futureValue shouldEqual ExtractedLinks(Set.empty, Set.empty)
  }

  it should "return no links if response is not html" in new Context {
    val notHtml: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, ByteString(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)))))

    service.extractLinks(notHtml).futureValue shouldEqual ExtractedLinks(Set.empty, Set.empty)
  }

  it should "return failure response future failed" in new Context {
    val exception = new RuntimeException
    val failedResponse: Future[HttpResponse] = Future.failed(exception)

    service.extractLinks(failedResponse).failed.futureValue shouldEqual exception
  }

  it should "return no links if they were clipped because site is too big" in new Context {
    val pageWithLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithLinks, 4)))

    new LinkExtractor(defaultConf.copy(maxPageSize = 5)).extractLinks(pageWithLinksResponse).futureValue shouldEqual ExtractedLinks(Set.empty, Set.empty)
  }

  it should "return links if they were not clipped on too big site" in new Context {
    val pageWithLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithLinks, 4)))

    new LinkExtractor(defaultConf.copy(maxPageSize = 135)).extractLinks(pageWithLinksResponse).futureValue shouldEqual ExtractedLinks(Set("http://pl.pl"), Set.empty)
  }

  it should "check for response code" in new Context {
    val pageWithLinksResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(status = StatusCodes.NotFound, entity = HttpEntity.fromPath(ContentTypes.`text/html(UTF-8)`, pageWithLinks, 4)))

    service.extractLinks(pageWithLinksResponse).failed.futureValue shouldBe a[RuntimeException]
  }

  trait Context {
    val pageWithLinks: Path = Paths.get(getClass.getResource(s"/html_pages/pageWithLink.html").getPath)
    val pageWithoutLink: Path = Paths.get(getClass.getResource(s"/html_pages/pageWithoutLink.html").getPath)

    implicit val actorSystem = ActorSystem("test-system")
    implicit val materializer = ActorMaterializer()

    val service = new LinkExtractor(defaultConf)
  }
}
