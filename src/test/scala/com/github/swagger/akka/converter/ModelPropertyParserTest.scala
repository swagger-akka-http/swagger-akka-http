package com.github.swagger.akka.converter

import io.swagger.converter._
import io.swagger.models.properties
import models._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class ModelPropertyParserTest extends FlatSpec with Matchers {

  it should "process Model with Scala Option Int" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[ModelWOptionInt]).asScala.toMap
    val model = schemas.get("ModelWOptionInt")
    model should be ('defined)
    val optBoolean = model.get.getProperties().get("optInt")
    optBoolean should not be (null)
    optBoolean shouldBe a [properties.DecimalProperty]
    optBoolean.getRequired should be (false)
  }
}