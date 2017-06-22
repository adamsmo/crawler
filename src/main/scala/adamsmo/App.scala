package adamsmo

import adamsmo.actors.LinkDispatcherActor.StartCrawling
import adamsmo.common.CmdParser._
import adamsmo.common.Logger

import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

object App {
  def main(args: Array[String]): Unit = {

    parser.parse(args, Command()) match {
      case Some(Command(depth, urls)) =>
        new Crawler with Logger {
          linkDispatcherActor ! StartCrawling(depth, urls)

          Runtime.getRuntime.addShutdownHook(new Thread() {
            override def run(): Unit = {

              Try(Await.ready(actorSystem.terminate, configuration.shutdownTimeout)) match {
                case Failure(e) => log.error("Fail to properly shutdown crawler, because of exception:", e)
                case Success(_) => log.info("Crawler shutdown correctly")
              }
            }
          })
        }
      case _ =>
    }
  }
}
