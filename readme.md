## Simple web crawler written in scala

### Usage


build with:

`sbt sbt assembly`

this will create file `target/scala-2.12/crawler.jar`

to show help message use `--help`

```
java -jar target/scala-2.12/crawler.jar --help

example calls:
 sbt "run --depth 1 --urls http://www.google.pl,http://www.google.com"
 java -jar --depth 1 --urls http://www.google.pl,http://www.google.com

Usage: crawler [options]

  --depth number     depth to which crawler should follow links on page, 0 mean that it will only load initial url
  --urls url,url...  list of urls to crawl separated by comas, urls have to be prefixed with http or https,duplicated urls will be treated as one
  --help             prints this usage text

```


This implementation follows absolute and relative links that it finds on page.
By default it supports pages of size up to 1 megabyte, bigger pages are clipped to 1 megabyte.
By default it ignores links longer than 200 characters.
Crawler travers pages depth-first until it reaches set max depth.

After it finishes it prints to console directed graphs for each specified url in form of list of grouped edges, each group contains start and 1-N ends
Example graph in form of grouped edges:
```
start A ends B C
start B ends A C
```

graph:
```
     C
    ↗  ↖
   A -> B
     <-
```

## Configuration

Crawler can be configured with `application.conf` and with `-D` options it uses standard scala configuration
Default values for configurations options are

```
  max-page-size = 1048576
  max-url-length = 200
  response-time-out = 10s
  max-number-of-retry = 2
  max-parallel-requests = 40
  shutdown-timeout = 10s
```

## Possible improvements:
* Add delay to requests to prevent banning by server
* Better graph printing
* Limit pools in LinkDispatcherActor to prevent out of memory

## Error message in logs when crawling https sites
When stopping crawler after visiting https site in logs you may see:
```
[ERROR] ... [akka.actor.ActorSystemImpl(crawler-actor-system)] Outgoing request stream error (akka.stream.AbruptTerminationException)
```
this is known harmless error in akka http
related ticket: https://github.com/akka/akka-http/issues/907