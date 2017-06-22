package adamsmo

import adamsmo.actors.LinkDispatcherActor
import adamsmo.conf.Configuration
import adamsmo.http.LinkExtractor
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer

import scala.concurrent.duration.{FiniteDuration, _}

trait SystemBuilder {
  implicit lazy val actorSystem = ActorSystem("crawler-actor-system")
}

trait MaterializerBuilder {
  self: SystemBuilder =>
  implicit lazy val mat = ActorMaterializer()
}

trait LinkExtractorBuilder {
  self: ConfigurationBuilder with
    MaterializerBuilder =>
  lazy val linkExtractor = new LinkExtractor(configuration)
}

trait ConfigurationBuilder {
  //todo change to text file
  lazy val configuration = new Configuration {
    override def maxNumberOfRetry: Int = 2

    override def maxPageSize: Int = 100 * 1024

    override def maxParallelRequests: Int = 10

    override def responseTimeOut: FiniteDuration = 10.seconds

    override def shutdownTimeout: FiniteDuration = 10.seconds

    override def maxUrlLength: Int = 200
  }
}

trait LinkDispatcherActorBuilder {
  self: SystemBuilder with
    LinkExtractorBuilder with
    ConfigurationBuilder =>
  lazy val linkDispatcherActor: ActorRef = actorSystem.actorOf(LinkDispatcherActor.constructProps(linkExtractor, configuration), "linkDispatcher")
}

trait Crawler extends
  SystemBuilder with
  MaterializerBuilder with
  LinkExtractorBuilder with
  ConfigurationBuilder with
  LinkDispatcherActorBuilder