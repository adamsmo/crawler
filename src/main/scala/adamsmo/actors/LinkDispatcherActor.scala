package adamsmo.actors

import java.security.MessageDigest

import adamsmo.actors.LinkDispatcherActor._
import adamsmo.conf.Configuration
import adamsmo.http.LinkExtractor
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Scheduler}

import scala.concurrent.ExecutionContext.Implicits.global

class LinkDispatcherActor(linkExtractor: LinkExtractor, conf: Configuration) extends Actor with ActorLogging {
  val scheduler: Scheduler = context.system.scheduler

  override def receive: Receive = {
    case req: StartCrawling =>
      val initialState = preapreState(sender(), req)
      context become crawlUrls(initialState)
      self ! ScheduleUrls
  }

  def crawlUrls(state: State): Receive = {
    case ScheduleUrls =>
      import state._

      if (pendingUrls.size <= conf.maxParallelRequests && urlsToVisit.nonEmpty) {
        val scheduleLimit = conf.maxParallelRequests - pendingUrls.size
        val toSchedule = urlsToVisit.take(scheduleLimit)
        val newPending = toSchedule.map { link =>

          val md = MessageDigest.getInstance("SHA-1")
          val hash = md.digest(link.url.getBytes).map("%02x".format(_)).mkString("")

          val child = context.actorOf(LinkExtractorActor.constructProps(link.url, linkExtractor, conf), s"linkExtractor-$hash-${link.retries}")
          val childTimeout = scheduler.scheduleOnce(conf.responseTimeOut, self, FailedUrl(child))
          child -> (link, childTimeout)
        }
        context become crawlUrls(state.copy(pendingUrls = pendingUrls ++ newPending, urlsToVisit = urlsToVisit.drop(scheduleLimit)))
      }
      self ! CheckDone

    case FailedUrl(actor) =>
      import state._

      val (url, timeout) = pendingUrls(actor)
      timeout.cancel()
      context.stop(actor)

      val retried = url.retries + 1

      if (retried <= conf.maxNumberOfRetry) {
        context become crawlUrls(state.copy(pendingUrls = pendingUrls - actor, urlsToVisit = url.copy(retries = retried) +: urlsToVisit))
      } else {
        context become crawlUrls(state.copy(pendingUrls = pendingUrls - actor))
      }

      self ! ScheduleUrls
      self ! CheckDone

    case ExtractedUrls(actor, urls) =>
      import state._

      val (url, timeout) = pendingUrls(actor)
      timeout.cancel()
      context.stop(actor)

      log.debug(s"for link $url got new urls $urls")

      val filter = (pendingUrls.values.map { case (link, _) => link.url } ++ visitedUrls).toSet
      val newUrlsToVisit = urls.diff(filter).map(s => Link(s, url.depth + 1, 0)).filter(_.depth <= request.maxDepth).toSeq

      context become crawlUrls(state.copy(
        graphs = graphs.take(1).map { case (rootUrl, graph) => (rootUrl, graph + Edges(url.url, urls.toSeq)) } ++ graphs.drop(1),
        pendingUrls = pendingUrls - actor,
        visitedUrls = visitedUrls + url.url,
        urlsToVisit = newUrlsToVisit ++ urlsToVisit))

      self ! ScheduleUrls
      self ! CheckDone

    case CheckDone =>
      import state._

      if (pendingUrls.isEmpty && urlsToVisit.isEmpty) {
        if (request.initialUrls.isEmpty) {
          context become done(state)
          self ! Report
        } else {
          val nextState = preapreState(client, request)
          context become crawlUrls(nextState.copy(graphs = nextState.graphs ++ graphs))
          self ! ScheduleUrls
        }
      }
  }

  def done(state: State): Receive = {
    case Report =>
      import state._

      printGraph(graphs)
      client ! DoneCrawling(graphs.reverse)

      if (request.singleRun) {
        context.system.terminate
      } else {
        context become receive
      }
  }

  private def printGraph(graphs: Seq[(String, Set[Edges])]): Unit = {
    graphs.foreach { case (url, graph) =>
      log.info("------------------------------------------")
      log.info(s"got graph for: $url")
      graph.foreach(e => log.info(s"edges starts in: [${e.start}] and ends in [${e.ends}]"))
      log.info("------------------------------------------")
    }
  }

  private def preapreState(sender: ActorRef, request: StartCrawling) = State(
    client = sender,
    request = request.copy(initialUrls = request.initialUrls.drop(1)),
    graphs = request.initialUrls.take(1).map(u => (u, Set.empty[Edges])),
    pendingUrls = Map.empty,
    visitedUrls = Set.empty,
    urlsToVisit = request.initialUrls.take(1).map(u => Link(url = u, depth = 0, retries = 0)))

  private case class State(
    client: ActorRef,
    request: StartCrawling,
    graphs: Seq[(String, Set[Edges])],
    pendingUrls: Map[ActorRef, (Link, Cancellable)],
    visitedUrls: Set[String],
    urlsToVisit: Seq[Link]
  )

}

object LinkDispatcherActor {

  def constructProps(linkExtractor: LinkExtractor, conf: Configuration): Props = Props(new LinkDispatcherActor(linkExtractor, conf))

  case object Report

  case object CheckDone

  case object ScheduleUrls

  case class FailedUrl(actor: ActorRef)

  case class StartCrawling(maxDepth: Int, initialUrls: Seq[String], singleRun: Boolean = true)

  case class DoneCrawling(graphs: Seq[(String, Set[Edges])])

  case class Link(url: String, depth: Int, retries: Int)

  //todo have to be one to one edge - start a end b start a end b c <- will never happen because we are asking for node children only once per graph
  case class Edges(start: String, ends: Seq[String])

  case class ExtractedUrls(actor: ActorRef, urls: Set[String])

}