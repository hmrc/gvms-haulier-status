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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{eq => mEq}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.gvmshaulierstatus.error.HaulierStatusError.{CorrelationIdAlreadyExists, CorrelationIdNotFound}
import uk.gov.hmrc.gvmshaulierstatus.helpers.BaseControllerSpec
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId

import scala.concurrent.Future

class HaulierStatusControllerSpec extends BaseControllerSpec {

  trait Setup {
    val controller = new HaulierStatusController(mockHaulierStatusService, stubControllerComponents())
  }

  "create" should {
    "return 201 Created if correlation id does not exist" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.create(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val result: Future[Result] = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result) shouldBe CREATED

      verify(mockHaulierStatusService).create(mEq(correlationId))
    }

    "return 400 Bad Request if correlation id already exists" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.create(mEq(correlationId))).thenReturn(EitherT.leftT(CorrelationIdAlreadyExists))

      val result: Future[Result] = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include(s"An entry with correlation id ${correlationId.id} already exists")

      verify(mockHaulierStatusService).create(mEq(correlationId))
    }

    "return 400 Bad Request if payload has unexpected JSON structure" in new Setup {
      val correlationId: String = "corr-id-1"

      val result: Future[Result] = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result) shouldBe BAD_REQUEST

      verifyNoInteractions(mockHaulierStatusService)
    }
  }

  "update" should {
    "return 200 Ok after successfully updating if correlation id exists" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.update(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val result: Future[Result] = controller.update(correlationId)(fakeRequest)

      status(result) shouldBe OK

      verify(mockHaulierStatusService).update(mEq(correlationId))
    }

    "return 404 Not Found if correlation id does not exist" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.update(mEq(correlationId))).thenReturn(EitherT.leftT(CorrelationIdNotFound))

      val result: Future[Result] = controller.update(correlationId)(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include(s"No entry with correlation id ${correlationId.id} found")

      verify(mockHaulierStatusService).update(mEq(correlationId))
    }
  }
}
