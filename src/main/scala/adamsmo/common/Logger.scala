package adamsmo.common

import org.slf4j.{Logger => Log}
import org.slf4j.LoggerFactory

trait Logger {
  val log: Log = LoggerFactory.getLogger(getClass)
}
