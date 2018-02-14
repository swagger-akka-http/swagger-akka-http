package models

import io.swagger.annotations.ApiModelProperty
import scala.annotation.meta.field

case class ModelWOptionBigDecimal(
           @(ApiModelProperty @field)(value="this is an Option[BigDecimal] attribute") optBigDecimal: Option[BigDecimal])
