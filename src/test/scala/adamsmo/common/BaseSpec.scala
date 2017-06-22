package adamsmo.common

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

trait BaseSpec extends FlatSpec with Matchers with ScalaFutures with PatienceConfiguration {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(50, Millis))

  val defaultConf = MockConf(
    maxPageSize = 5 * 1000,
    responseTimeOut = 3.seconds,
    maxNumberOfRetry = 5,
    maxParallelRequests = 10,
    shutdownTimeout = 10.seconds,
    maxUrlLength = 200
  )
}
