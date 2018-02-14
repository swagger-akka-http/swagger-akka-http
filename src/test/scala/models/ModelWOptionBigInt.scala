package models

import io.swagger.annotations.ApiModelProperty
import scala.annotation.meta.field

case class ModelWOptionBigInt(
           @(ApiModelProperty @field)(value="this is an Option[BigInt] attribute") optBigInt: Option[BigInt])
