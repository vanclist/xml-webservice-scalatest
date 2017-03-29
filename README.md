## xml-webservice-scalatest

##### This project is to demonstrate ease of creation a functional tests harness for typical XML-generating web service using ScalaTest

Let's assume that we have a digital ad web service which responds with XML document that complies to [VAST schema](https://en.wikipedia.org/wiki/Video_Ad_Serving_Template "en.wikipedia.org/VAST")
The service will serve next functionality:
- load ad campaigns configuration from some DB and create matching index
- receive ad requests from video player and match query params with ad campaign index
- respond with VAST-formatted XML document
- receive tracking events from video player
- store ad requests and tracking events stats to persistent storage

For the sake of real-life example, such web service uses MySQL as configuration source and MongoDB for 
storing statistics. As ad campaigns configuration is usually refreshed by some schedule, i.e. 10 or 30 minutes, 
for testing need we can have some "handles" for forced update. If the service is based on JVM, it could be JMX bean.

Scalatest provides concise stdout:
```
FixedPricePaymentType:
- Campaign should be filtered by price if publisher's clientId is internal client (2 seconds, 535 milliseconds)
- Campaign should be filtered by price if publisher's clientId is internal client (3 seconds, 81 milliseconds)
SecondPricePaymentType:
- Second price payment type: price filter is passed for default query params (2 seconds, 946 milliseconds)
ExchangeTypeTest:
- CDN exchange: single campaign with wrapper VAST (2 seconds, 848 milliseconds)
- CDN exchange: multiple campaigns with wrapper VASTs (3 seconds, 919 milliseconds) (pending)
- CDN exchange: wrapper campaign + inline campaign (pending)
Run completed in 14 seconds.
Total number of tests run: 5
Suites: completed 3, aborted 0
Tests: succeeded 5, failed 0, canceled 0, ignored 0, pending 1
All tests passed.
```
Simple HTML report will generated as well (see [examples](https://github.com/vanclist/xml-webservice-scalatest/tree/master/examples/report "/examples")), which can be attached to your CI task.
