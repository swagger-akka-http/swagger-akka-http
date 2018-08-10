package models

import io.swagger.v3.oas.annotations.Operation

import scala.annotation.meta.field

case class CoreBug814 (
  @(Operation @field)(description="true if open to the public") isFoo: Boolean
)