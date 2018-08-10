package models

import io.swagger.v3.oas.annotations.Parameter

case class ModelWOptionString (
           stringOpt: Option[String],
           stringWithDataTypeOpt: Option[String])


case class ModelWOptionModel (modelOpt: Option[ModelWOptionString])

case class ModelWithOptionAndNonOption(required: String,
                                       optional: Option[String],
                                       @Parameter(required = false) forcedOptional: String,
                                       @Parameter(required = true) forcedRequired: Option[String]
                                      )
