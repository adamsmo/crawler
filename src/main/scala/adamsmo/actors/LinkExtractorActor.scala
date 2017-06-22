package adamsmo.actors

import adamsmo.actors.LinkDispatcherActor.{ExtractedUrls, FailedUrl}
import adamsmo.conf.Configuration
import adamsmo.http.{ExtractedLinks, LinkExtractor}
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.pattern.pipe
import akka.stream.ActorMaterializer

import scala.concurrent.Future

class LinkExtractorActor(url: String, linkExtractor: LinkExtractor, conf: Configuration)
  extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()

  val http = Http(context.system)

  val scheduler: Scheduler = context.system.scheduler

  override def preStart(): Unit = {
    import context.dispatcher
    val response = http.singleRequest(HttpRequest(uri = url))
    val extracted: Future[ExtractedLinks] = linkExtractor.extractLinks(response)
    extracted pipeTo self
  }

  override def receive: Receive = {
    case ExtractedLinks(absolute, relative) =>
      context.parent ! ExtractedUrls(self, (relative.map(path => url + path) ++ absolute).filter(_.length <= conf.maxUrlLength))
      context stop self

    case Failure(exception) =>
      log.debug(s"fail to get links from $url because of $exception")
      context.parent ! FailedUrl(self)
      context stop self
  }
}

object LinkExtractorActor {
  def constructProps(url: String, linkExtractor: LinkExtractor, conf: Configuration): Props = Props(new LinkExtractorActor(url, linkExtractor, conf))
}