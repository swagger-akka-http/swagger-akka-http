package models

import io.swagger.annotations.{ ApiModel, ApiModelProperty }
import models.OrderSize.OrderSize

import scala.annotation.meta.field

@ApiModel(description = "Scala model containing an Enumeration Value that is annotated with the dataType of the Enumeration class")
case class SModelWithEnum(
  // @(ApiModelProperty @field)(value = "Textual label") label: Option[String] = None,
  @(ApiModelProperty @field)(value = "Order Size", dataType = "models.OrderSize$") orderSize: OrderSize = OrderSize.TALL)

case object OrderSize extends Enumeration(0) {
  type OrderSize = Value
  val TALL = Value("TALL")
  val GRANDE = Value("GRANDE")
  val VENTI = Value("VENTI")
}