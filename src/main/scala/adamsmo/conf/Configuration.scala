package adamsmo.conf

import scala.concurrent.duration.FiniteDuration

trait Configuration {


  /**
    *
    * @return maximal allowed page size in bytes
    */
  def maxPageSize: Int

  /**
    *
    * @return maximal length of string representing url
    */
  def maxUrlLength: Int

  /**
    *
    * @return http response time out
    */
  def responseTimeOut: FiniteDuration

  /**
    *
    * @return maximum number of request retries for given url
    */
  def maxNumberOfRetry: Int

  /**
    *
    * @return maximum number of paralle http requests
    */
  def maxParallelRequests: Int

  /**
    *
    * @return actor system time out for shutdown
    */
  def shutdownTimeout: FiniteDuration
}
