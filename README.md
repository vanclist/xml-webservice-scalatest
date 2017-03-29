## xml-webservice-scalatest

##### This project demonstrates ease of creation ф functional tests harness for typical XML-generating web service using ScalaTest

Let's assume that we have a digital ad web service which responds with XML document. The document complies to VAST schema https://en.wikipedia.org/wiki/Video_Ad_Serving_Template
The service will serve next functionality:
* load ad campaigns configuration from some DB and create matching index
* receive ad requests from video player and match query params with ad campaign index
* respond with VAST-formatted XML document
* receive tracking events from video player
* store ad requests and tracking events stats to persistent storage

For the sake of real-life example, such web service uses MySQL as configuration source and MongoDB for storing statistics.
As ad campaigns configuration is usually refreshed by some schedule, i.e. 10 or 30 minutes, for testing need we can have some "handles" for forced update. 
If the service is based on JVM, it could be JMX bean.