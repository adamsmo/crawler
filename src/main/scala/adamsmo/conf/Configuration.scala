package adamsmo.conf

trait Configuration {
  /**
    *
    * @return maximal allowed page size in bytes
    */
  def maxPageSize: Int
}
