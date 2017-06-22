package adamsmo.common

import adamsmo.conf.Configuration

import scala.concurrent.duration.FiniteDuration

case class MockConf(
  maxPageSize: Int,
  responseTimeOut: FiniteDuration,
  maxNumberOfRetry: Int,
  maxParallelRequests: Int,
  shutdownTimeout: FiniteDuration,
  maxUrlLength: Int) extends Configuration
