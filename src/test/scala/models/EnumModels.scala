package models

import io.swagger.v3.oas.annotations.media.Schema
import models.OrderSize.OrderSize

import scala.annotation.meta.field

@Schema(description = "Scala model containing an Enumeration Value that is annotated with the dataType of the Enumeration class")
case class SModelWithEnum(
  @(Schema @field)(name = "Order Size", implementation = classOf[OrderSize]) orderSize: OrderSize = OrderSize.TALL)

case object OrderSize extends Enumeration(0) {
  type OrderSize = Value
  val TALL = Value("TALL")
  val GRANDE = Value("GRANDE")
  val VENTI = Value("VENTI")
}