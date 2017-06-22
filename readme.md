#Simple web crawler written in scala

This implementation follows absolute and relative links that it finds on page.

#Possible improvements:
* Add delay to requests to prevent banning by server
* Better graph printing

#Error message in logs when crawling https sites
When stopping crawler after visiting https site in logs you may see:
```
[ERROR] ... [akka.actor.ActorSystemImpl(crawler-actor-system)] Outgoing request stream error (akka.stream.AbruptTerminationException)
```
this is known harmless error in akka http
related ticket: https://github.com/akka/akka-http/issues/907