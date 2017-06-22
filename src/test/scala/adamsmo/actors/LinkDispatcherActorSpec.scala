package adamsmo.actors

import adamsmo.actors.LinkDispatcherActor.{DoneCrawling, Edges, StartCrawling}
import adamsmo.common.BaseSpec
import adamsmo.http.{ExtractedLinks, LinkExtractor}
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

class LinkDispatcherActorSpec extends BaseSpec {

  "LinkDispatcherActor" should "return empty result for empty list of initial url" in new Context {
    linkDispatcherActor ! StartCrawling(1, Seq())

    probe.expectMsg(waitTimeOut, DoneCrawling(Nil))
  }

  it should "return initial url links only for depth 0" in new Context {
    linkDispatcherActor ! StartCrawling(0, Seq(url))

    (linkExtractor.extractLinks _).expects(*).returns(Future.successful(ExtractedLinks(Set(url, anotherUrl), Set.empty)))

    probe.expectMsg(waitTimeOut, DoneCrawling(Seq((url, Set(Edges(url, Seq(url, anotherUrl)))))))
  }

  it should "return 2 graphs with initial url links only for depth 0" in new Context {

    linkDispatcherActor ! StartCrawling(0, Seq(url, anotherUrl))

    (linkExtractor.extractLinks _).expects(*).returns(Future.successful(ExtractedLinks(Set(url), Set.empty)))
    (linkExtractor.extractLinks _).expects(*).returns(Future.successful(ExtractedLinks(Set(url), Set.empty)))

    probe.expectMsg(waitTimeOut, DoneCrawling(Seq(
      (url, Set(Edges(url, Seq(url)))),
      (anotherUrl, Set(Edges(anotherUrl, Seq(url))))
    )))
  }

  it should "not follow recursive links" in new Context {
    linkDispatcherActor ! StartCrawling(15, Seq(url))

    (linkExtractor.extractLinks _).expects(*).returns(Future.successful(ExtractedLinks(Set(url), Set.empty)))

    probe.expectMsg(waitTimeOut, DoneCrawling(Seq((url, Set(Edges(url, Seq(url)))))))
  }

  it should "retry on failure" in new Context {
    linkDispatcherActor ! StartCrawling(15, Seq(url))

    (linkExtractor.extractLinks _).expects(*).returns(Future.failed(new RuntimeException))
    (linkExtractor.extractLinks _).expects(*).returns(Future.successful(ExtractedLinks(Set(url), Set.empty)))

    probe.expectMsg(waitTimeOut, DoneCrawling(Seq((url, Set(Edges(url, Seq(url)))))))
  }

  it should "give up after max retry" in new Context {
    override val linkDispatcherActor: ActorRef =
      actorSystem.actorOf(LinkDispatcherActor.constructProps(linkExtractor, defaultConf.copy(maxNumberOfRetry = 1)))

    linkDispatcherActor ! StartCrawling(15, Seq(url))

    (linkExtractor.extractLinks _).expects(*).returns(Future.failed(new RuntimeException))
    (linkExtractor.extractLinks _).expects(*).returns(Future.failed(new RuntimeException))

    probe.expectMsg(waitTimeOut, DoneCrawling(Seq((url, Set()))))
  }

  trait Context extends MockFactory {
    implicit val actorSystem = ActorSystem("linkExtractorActorTest-system")

    val waitTimeOut: FiniteDuration = 10.seconds
    val linkExtractor: LinkExtractor = mock[LinkExtractor]
    val linkDispatcherActor: ActorRef = actorSystem.actorOf(LinkDispatcherActor.constructProps(linkExtractor, defaultConf))

    val probe = TestProbe()
    implicit val sender: ActorRef = probe.ref

    val url = "fake"
    val anotherUrl: String = url + "/"
  }

}
