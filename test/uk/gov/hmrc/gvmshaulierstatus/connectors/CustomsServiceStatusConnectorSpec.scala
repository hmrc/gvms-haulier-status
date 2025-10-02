/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq as mEq}
import org.mockito.Mockito.*
import play.api.libs.json.Json
import uk.gov.hmrc.gvmshaulierstatus.helpers.BaseSpec
import uk.gov.hmrc.gvmshaulierstatus.models.State
import uk.gov.hmrc.gvmshaulierstatus.models.State.AVAILABLE
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.Future;

class CustomsServiceStatusConnectorSpec extends BaseSpec {

  "updateStatus" should {
    "return the http response" in {
      val mockHttpClient: HttpClientV2                  = mock[HttpClientV2]
      val connector:      CustomsServiceStatusConnector = new CustomsServiceStatusConnector(mockHttpClient, "http://localhost:0000")

      val state        = AVAILABLE.value
      val serviceId    = "haulier"
      val httpResponse = HttpResponse(200, body = "ok")

      when(mockHttpClient.put(mEq(url"http://localhost:0000/customs-service-status/services/$serviceId/status"))(any[HeaderCarrier]))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(mEq(Json.toJson(state)))(using any, any, any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val result = await(connector.updateStatus(serviceId, AVAILABLE))

      result shouldBe httpResponse

      verify(mockHttpClient, times(1)).put(mEq(url"http://localhost:0000/customs-service-status/services/$serviceId/status"))(any())
      verify(mockRequestBuilder).withBody(mEq(Json.toJson(AVAILABLE.value)))(using any, any, any)
      verify(mockRequestBuilder).execute[HttpResponse](using any(), any())
    }
  }
}
