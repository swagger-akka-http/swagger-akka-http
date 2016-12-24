/*
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
package com.github.swagger.akka

import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.HttpCharsets.`UTF-8`

object CustomMediaTypes {

  /**
    * [[http://media-types.ietf.narkive.com/emZX0ly2/proposed-media-type-registration-for-yaml]].
    */
  val `text/vnd.yaml`: MediaType.WithFixedCharset =
    MediaType.customWithFixedCharset("text", "vnd.yaml", `UTF-8`)
}
