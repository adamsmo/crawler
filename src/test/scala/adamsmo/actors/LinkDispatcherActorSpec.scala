package adamsmo.actors

import adamsmo.common.BaseSpec
import adamsmo.http.LinkExtractor
import akka.actor.ActorSystem
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration.{FiniteDuration, _}

class LinkDispatcherActorSpec extends BaseSpec {

  "LinkDispatcherActor" should "return empty result for empty list of initial url" in new Context {

  }

  it should "return initial url for depth 0" in new Context {

  }

  trait Context extends MockFactory {
    implicit val actorSystem = ActorSystem("linkExtractorActorTest-system")

    val waitTimeOut: FiniteDuration = 7.seconds
    val linkExtractor: LinkExtractor = mock[LinkExtractor]
    actorSystem.actorOf(LinkDispatcherActor.constructProps(linkExtractor, defaultConf))

    val url = "http://127.0.0.1"
  }

}
