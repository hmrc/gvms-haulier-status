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

package uk.gov.hmrc.gvmshaulierstatus.connectors

import play.api.libs.json.Json
import uk.gov.hmrc.gvmshaulierstatus.model.State
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse, StringContextOps}

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

@Singleton
class CustomsServiceStatusConnector @Inject() (
  httpClient:                                                    HttpClientV2,
  @Named("customsServiceStatusUrl") customsServiceStatusBaseUrl: String
)(implicit ec: ExecutionContext) {

  private val baseUrl = s"$customsServiceStatusBaseUrl/customs-service-status"

  implicit val rawReads: HttpReads[HttpResponse] = HttpReads.Implicits.throwOnFailure(HttpReads.Implicits.readEitherOf(HttpReads.Implicits.readRaw))

  def updateStatus(serviceId: String, state: State)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    httpClient.put(url"$baseUrl/services/$serviceId/status").withBody(Json.toJson(state)).execute[HttpResponse]
}
