/**
  * Copyright 2014 Getty Images, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.github.swagger.akka.samples

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._
import spray.json.DefaultJsonProtocol
import io.swagger.annotations.ApiModel

/**
  * @author rleibman
  */
trait ModelFormats
  extends DefaultJsonProtocol
    with SprayJsonSupport {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  implicit val dictEntryformats = jsonFormat3(DictEntry)
}
