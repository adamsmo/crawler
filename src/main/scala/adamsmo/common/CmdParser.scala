package adamsmo.common

object CmdParser {

  case class Command(depth: Int = 0, urls: Seq[String] = Nil)

  val parser = new scopt.OptionParser[Command]("crawler") {
    head("\nexample calls:\n", "sbt \"run --depth 1 --urls http://www.google.pl,http://www.google.com\"\n",
      "java -jar --depth 1 --urls http://www.google.pl,http://www.google.com\n")

    opt[Int]("depth").valueName("number").required().action { (x, c) =>
      c.copy(depth = x)
    } validate { value =>
      if (value < 0)
        failure("depth must be greater then 0")
      else
        success
    } text "depth to which crawler should follow links on page, 0 mean that it will only load initial url"

    opt[Seq[String]]("urls").valueName("url,url...").required().action((x, c) =>
      c.copy(urls = x.distinct)).text("list of urls to crawl separated by comas, urls have to be prefixed with http or https," +
      "duplicated urls will be treated as one")

    help("help") text "prints this usage text"

    override def showUsageOnError = true

  }
}


