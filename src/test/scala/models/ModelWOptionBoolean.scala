package models

import io.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class ModelWOptionBoolean(
           @(ApiModelProperty @field)(value="this is an Option[Boolean] attribute") optBoolean: Option[Boolean])
