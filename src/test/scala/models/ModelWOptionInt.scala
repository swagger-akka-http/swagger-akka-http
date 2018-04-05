package models

import io.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class ModelWOptionInt(
                            @(ApiModelProperty @field)(value="this is an Option[Int] attribute") optInt: Option[Int])
