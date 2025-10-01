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

package uk.gov.hmrc.gvmshaulierstatus.controllers

import org.slf4j.MDC
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.gvmshaulierstatus.actions.AuthorisedAction
import uk.gov.hmrc.gvmshaulierstatus.error.HaulierStatusError.{CorrelationIdAlreadyExists, CorrelationIdNotFound}
import uk.gov.hmrc.gvmshaulierstatus.models.CorrelationId
import uk.gov.hmrc.gvmshaulierstatus.models.CorrelationId._
import uk.gov.hmrc.gvmshaulierstatus.services.HaulierStatusService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class HaulierStatusController @Inject() (
  haulierStatusService: HaulierStatusService,
  authorisedAction:     AuthorisedAction,
  cc:                   ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseHaulierStatusController(cc) {

  private val correlationIdHeader = "X-Correlation-Id"

  def create(): Action[JsValue] =
    authorisedAction.withInternalAuth.async(parse.json) { implicit request =>
      validateJson[CorrelationId] { correlationId =>
        MDC.put(correlationIdHeader, correlationId.id)
        haulierStatusService
          .create(correlationId)
          .fold(
            { case CorrelationIdAlreadyExists =>
              BadRequest(s"An entry with correlation id ${correlationId.id} already exists")
            },
            _ => Created
          )
      }
    }

  def update(correlationId: CorrelationId): Action[AnyContent] =
    authorisedAction.withInternalAuth.async { _ =>
      MDC.put(correlationIdHeader, correlationId.id)
      haulierStatusService
        .update(correlationId)
        .fold(
          { case CorrelationIdNotFound =>
            NotFound(s"No entry with correlation id ${correlationId.id} found")
          },
          _ => Ok
        )
    }
}
