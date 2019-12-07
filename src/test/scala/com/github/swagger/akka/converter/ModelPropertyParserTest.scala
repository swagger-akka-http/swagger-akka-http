package com.github.swagger.akka.converter

import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util

import io.swagger.converter._
import io.swagger.models.{Model, properties}
import io.swagger.models.properties._
import io.swagger.scala.converter.SwaggerScalaModelConverter
import models._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class ModelPropertyParserTest extends FlatSpec with Matchers {
  it should "verify swagger-core bug 814" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[CoreBug814])
    val model = schemas.get("CoreBug814")
    model should not be (null)
    val isFoo = model.getProperties().get("isFoo")
    isFoo should not be (null)
    isFoo shouldBe a [BooleanProperty]
  }

  it should "process Option[String] as string" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[ModelWOptionString]).asScala.toMap
    val model = schemas.get("ModelWOptionString")
    model should be ('defined)
    val stringOpt = model.get.getProperties().get("stringOpt")
    stringOpt should not be (null)
    stringOpt shouldBe a [StringProperty]
    stringOpt.getRequired shouldBe false
    val stringWithDataType = model.get.getProperties().get("stringWithDataTypeOpt")
    stringWithDataType should not be (null)
    stringWithDataType shouldBe a [StringProperty]
    stringWithDataType.getRequired shouldBe false
  }

  it should "process Option[Model] as Model" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[ModelWOptionModel]).asScala.toMap
    val model = schemas.get("ModelWOptionModel")
    model should be ('defined)
    val modelOpt = model.get.getProperties().get("modelOpt")
    modelOpt should not be (null)
    modelOpt shouldBe a [RefProperty]
  }

  it should "process Model with Scala BigDecimal as Number" in {
    case class TestModelWithBigDecimal(field: BigDecimal)

    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[TestModelWithBigDecimal]).asScala.toMap
    val model = findModel(schemas, "TestModelWithBigDecimal")
    model should be ('defined)
    val modelOpt = model.get.getProperties().get("field")
    modelOpt shouldBe a [properties.DecimalProperty]
    modelOpt.getRequired shouldBe true
  }

  it should "process Model with Scala BigInt as Number" in {
    case class TestModelWithBigInt(field: BigInt)

    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[TestModelWithBigInt]).asScala.toMap
    val model = findModel(schemas, "TestModelWithBigInt")
    model should be ('defined)
    val modelOpt = model.get.getProperties().get("field")
    modelOpt shouldBe a [properties.BaseIntegerProperty]
    modelOpt.getRequired shouldBe true
  }

  it should "process Model with Scala Option BigDecimal" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[ModelWOptionBigDecimal]).asScala.toMap
    val model = schemas.get("ModelWOptionBigDecimal")
    model should be ('defined)
    val optBigDecimal = model.get.getProperties().get("optBigDecimal")
    optBigDecimal should not be (null)
    optBigDecimal shouldBe a [properties.DecimalProperty]
    optBigDecimal.getRequired shouldBe false
  }

  it should "process Model with Scala Option BigInt" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[ModelWOptionBigInt]).asScala.toMap
    val model = schemas.get("ModelWOptionBigInt")
    model should be ('defined)
    val optBigDecimal = model.get.getProperties().get("optBigInt")
    optBigDecimal should not be (null)
    optBigDecimal shouldBe a [properties.BaseIntegerProperty]
    optBigDecimal.getRequired shouldBe false
  }

  it should "process Model with Scala Option Int" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[ModelWOptionInt]).asScala.toMap
    val model = schemas.get("ModelWOptionInt")
    model should be('defined)
    val optInt = model.get.getProperties().get("optInt")
    optInt should not be (null)
    optInt shouldBe a[properties.ObjectProperty]
    optInt.getRequired shouldBe false
  }

  it should "process Model with Scala Option Int (Schema Override)" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[ModelWOptionIntSchemaOverride]).asScala.toMap
    val model = schemas.get("ModelWOptionIntSchemaOverride")
    model should be('defined)
    val optInt = model.get.getProperties().get("optInt")
    optInt should not be (null)
    optInt shouldBe a[properties.IntegerProperty]
    optInt.getRequired shouldBe false
  }

  it should "process Model with Scala Option Boolean" in {
    val converter = ModelConverters.getInstance()
    val schemas = converter.readAll(classOf[ModelWOptionBoolean]).asScala.toMap
    val model = schemas.get("ModelWOptionBoolean")
    model should be ('defined)
    val optBoolean = model.get.getProperties().get("optBoolean")
    optBoolean should not be (null)
    optBoolean shouldBe a [properties.ObjectProperty]
    optBoolean.getRequired shouldBe false
  }

  it should "process all properties as required barring Option[_] or if overridden in annotation" in {
    val schemas = ModelConverters
      .getInstance()
      .readAll(classOf[ModelWithOptionAndNonOption])
      .asScala

    val model = schemas("ModelWithOptionAndNonOption")
    model should not be (null)

    val optional = model.getProperties().get("optional")
    optional.getRequired shouldBe false

    val required = model.getProperties().get("required")
    required.getRequired shouldBe true

    val forcedRequired = model.getProperties().get("forcedRequired")
    forcedRequired.getRequired shouldBe true

    val forcedOptional = model.getProperties().get("forcedOptional")
    forcedOptional.getRequired shouldBe false
  }

  it should "process all properties as required barring Option[_] or if overridden in annotation (dataType)" in {
    val schemas = ModelConverters
      .getInstance()
      .readAll(classOf[ModelWithOptionAndNonOption2])
      .asScala

    val model = schemas("ModelWithOptionAndNonOption2")
    model should not be (null)

    val optional = model.getProperties().get("optional")
    optional.getRequired shouldBe false

    val required = model.getProperties().get("required")
    required.getRequired shouldBe true

    val forcedRequired = model.getProperties().get("forcedRequired")
    forcedRequired.getRequired shouldBe true

    val forcedOptional = model.getProperties().get("forcedOptional")
    forcedOptional.getRequired shouldBe false
  }

  it should "handle null properties from converters later in the chain" in {
    object CustomConverter extends ModelConverter {
      def resolve(`type`: Type, context: ModelConverterContext, chain: util.Iterator[ModelConverter]): Model = {
        if (chain.hasNext) chain.next().resolve(`type`, context, chain) else null
      }

      def resolveProperty(`type`: Type, context: ModelConverterContext, annotations: Array[Annotation], chain: util.Iterator[ModelConverter]): Property = {
        null
      }
    }

    val converter = new ModelConverters()
    converter.addConverter(CustomConverter)
    converter.addConverter(new SwaggerScalaModelConverter)
    converter.readAll(classOf[Option[Int]])
  }

  def findModel(schemas: Map[String, Model], name: String): Option[Model] = {
    schemas.get(name) match {
      case Some(m) => Some(m)
      case None =>
        schemas.keys.find { case k => k.startsWith(name) } match {
          case Some(key) => schemas.get(key)
          case None => schemas.values.headOption
        }
    }
  }
}
