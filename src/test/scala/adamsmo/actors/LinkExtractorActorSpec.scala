package adamsmo.actors

import adamsmo.actors.LinkDispatcherActor.{ExtractedUrls, FailedUrl}
import adamsmo.common.BaseSpec
import adamsmo.http.{ExtractedLinks, LinkExtractor}
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Future
import scala.concurrent.duration._

class LinkExtractorActorSpec extends BaseSpec {
  "LinkExtractorActor" should "filter out too long urls" in new Context {
    val linkExtractorActor: ActorRef =
      probe.childActorOf(LinkExtractorActor.constructProps(url, linkExtractor, defaultConf.copy(maxUrlLength = 10)))

    (linkExtractor.extractLinks _).expects(*).returns(Future.successful(ExtractedLinks(Set("http://pl", "http://pl.pl"), Set("/random"))))

    probe.expectMsg(waitTimeOut, ExtractedUrls(linkExtractorActor, Set("http://pl")))
  }

  it should "returns failure when extraction fails" in new Context {
    val linkExtractorActor: ActorRef =
      probe.childActorOf(LinkExtractorActor.constructProps(url, linkExtractor, defaultConf))

    (linkExtractor.extractLinks _).expects(*).returns(Future.failed(new RuntimeException))

    probe.expectMsg(waitTimeOut, FailedUrl(linkExtractorActor))
  }

  trait Context extends MockFactory {
    implicit val actorSystem = ActorSystem("linkExtractorActorTest-system")

    val waitTimeOut: FiniteDuration = 7.seconds
    val probe = TestProbe()
    val linkExtractor: LinkExtractor = mock[LinkExtractor]
    val url = "http://127.0.0.1"
  }

}
