package com.github.swagger.akka.samples

import javax.ws.rs.Path

abstract class TestApiWithNoAnnotation

@Path("/test")
object TestApiWithObject {
  //@ApiOperation(value = "testApiOperation", httpMethod = "GET")
  def testOperation = {}
}

