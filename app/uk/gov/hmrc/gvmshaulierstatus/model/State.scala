/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gvmshaulierstatus.model

import play.api.libs.json._

sealed trait State {
  val value: String = toString
}

object State {
  case object AVAILABLE extends State
  case object UNAVAILABLE extends State
  case object UNKNOWN extends State

  implicit val format: Format[State] = new Format[State] {

    override def writes(o: State): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[State] =
      json.validate[String].flatMap {
        case AVAILABLE.value   => JsSuccess(AVAILABLE)
        case UNAVAILABLE.value => JsSuccess(UNAVAILABLE)
        case UNKNOWN.value     => JsSuccess(UNKNOWN)
        case e                 => JsError(s"invalid value: $e for State type")
      }
  }
}
