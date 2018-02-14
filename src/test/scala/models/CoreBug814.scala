package models

import io.swagger.annotations.{ ApiModel, ApiModelProperty }
import scala.annotation.meta.field

case class CoreBug814 (
  @(ApiModelProperty @field)(required = true, value="true if open to the public") isFoo: Boolean
)