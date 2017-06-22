package adamsmo

import adamsmo.actors.LinkDispatcherActor
import adamsmo.conf.Configuration
import adamsmo.http.LinkExtractor
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

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
  val config: Config = ConfigFactory.load().getConfig("crawler")

  lazy val configuration = new Configuration {
    val maxNumberOfRetry: Int = config.getInt("max-number-of-retry")

    val maxPageSize: Int = config.getInt("max-page-size")

    val maxParallelRequests: Int = config.getInt("max-parallel-requests")

    val responseTimeOut: FiniteDuration = config.getDuration("response-time-out").toMillis.millis

    val shutdownTimeout: FiniteDuration = config.getDuration("shutdown-timeout").toMillis.millis

    val maxUrlLength: Int = config.getInt("max-url-length")
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