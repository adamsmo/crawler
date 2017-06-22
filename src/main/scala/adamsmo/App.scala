package adamsmo

import adamsmo.actors.LinkDispatcherActor.{DoneCrawling, StartCrawling}
import adamsmo.common.Logger
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object App {
  def main(args: Array[String]): Unit = {
    val crawler = new Crawler with Logger {

      implicit val timeOut = Timeout.zero

      linkDispatcherActor ! StartCrawling(3, Seq("http://www.google.com"))

      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {

          Try(Await.ready(actorSystem.terminate, configuration.shutdownTimeout)) match {
            case Failure(e) => log.error("Fail to properly shutdown crawler, because of exception:", e)
            case Success(_) => log.info("Crawler shutdown correctly")
          }
        }
      })
    }
  }
}
