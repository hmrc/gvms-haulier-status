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

package uk.gov.hmrc.gvmshaulierstatus.models.documents

import cats.Eq
import cats.implicits.catsSyntaxEq
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import uk.gov.hmrc.gvmshaulierstatus.models.documents.HaulierStatusDocument.timeFormatter
import uk.gov.hmrc.gvmshaulierstatus.models.documents.Status.Created
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

sealed trait Status {
  val value: String = toString
}

object Status {

  implicit val statusEq: Eq[Status] =
    Eq.instance[Status] { (status1, status2) =>
      status1.value === status2.value
    }

  case object Created extends Status
  case object Received extends Status

  implicit val format: Format[Status] = new Format[Status] {

    override def writes(o: Status): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[Status] =
      json.validate[String].flatMap {
        case Created.value  => JsSuccess(Created)
        case Received.value => JsSuccess(Received)
        case e              => JsError(s"invalid value: $e for Status type")
      }
  }
}

case class HaulierStatusDocument(id: String, status: Status, createdAt: Instant, lastUpdatedAt: Instant) {

  override def toString: String =
    id + ", " +
      s"CREATED ${timeFormatter.format(createdAt)}, " +
      s"RECEIVED ${timeFormatter.format(lastUpdatedAt)}"
}

object HaulierStatusDocument {

  val mongoFormat: OFormat[HaulierStatusDocument] = {
    implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

    val read: Reads[HaulierStatusDocument] = (
      (JsPath \ "id").read[String] and
        (JsPath \ "status").readWithDefault[Status](Created) and
        (JsPath \ "createdAt").read[Instant] and
        (JsPath \ "lastUpdatedAt").read[Instant].orElse((JsPath \ "createdAt").read[Instant])
    )((id, status, createdAt, lastUpdatedAt) => HaulierStatusDocument(id, status, createdAt, lastUpdatedAt))

    OFormat[HaulierStatusDocument](read, Json.writes[HaulierStatusDocument])
  }

  implicit val format: OFormat[HaulierStatusDocument] = Json.format[HaulierStatusDocument]

  val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM:dd HH:mm:ss:SSS").withZone(ZoneId.of("Europe/London"))
}
