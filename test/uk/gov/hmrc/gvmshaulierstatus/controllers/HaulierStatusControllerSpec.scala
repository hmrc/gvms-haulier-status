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
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito.{never, verify, verifyNoInteractions, when}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.gvmshaulierstatus.actions.AuthorisedAction
import uk.gov.hmrc.gvmshaulierstatus.error.HaulierStatusError.{CorrelationIdAlreadyExists, CorrelationIdNotFound}
import uk.gov.hmrc.gvmshaulierstatus.helpers.BaseControllerSpec
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.Future

class HaulierStatusControllerSpec extends BaseControllerSpec {

  trait Setup {

    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()
    val expectedPermissionPredicate: Predicate.Permission =
      Predicate.Permission(
        Resource(
          ResourceType("gvms-haulier-status"),
          ResourceLocation("*")
        ),
        IAAction("WRITE")
      )
    val mockStubBehaviour: StubBehaviour = mock[StubBehaviour]
    when(mockStubBehaviour.stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    val backendAuthComponentsStub: BackendAuthComponents = BackendAuthComponentsStub(mockStubBehaviour)
    val mockAuthorisedAction = new AuthorisedAction(backendAuthComponentsStub)
    val controller           = new HaulierStatusController(mockHaulierStatusService, mockAuthorisedAction, cc)
  }

  "create" should {
    "return 201 Created if correlation id does not exist" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.create(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val result: Future[Result] = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result) shouldBe CREATED

      verify(mockHaulierStatusService).create(mEq(correlationId))
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 400 Bad Request if correlation id already exists" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.create(mEq(correlationId))).thenReturn(EitherT.leftT(CorrelationIdAlreadyExists))

      val result: Future[Result] = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include(s"An entry with correlation id ${correlationId.id} already exists")

      verify(mockHaulierStatusService).create(mEq(correlationId))
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 400 Bad Request if payload has unexpected JSON structure" in new Setup {
      val correlationId: String = "corr-id-1"

      val result: Future[Result] = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result) shouldBe BAD_REQUEST

      verifyNoInteractions(mockHaulierStatusService)
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 401 Unauthorized if internal-auth fails to authenticate (expired or invalid token)" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockStubBehaviour.stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval)))
        .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))
      when(mockHaulierStatusService.create(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val result: Future[Result] = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result) shouldBe UNAUTHORIZED

      verify(mockHaulierStatusService, never).create(mEq(correlationId))
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 401 Unauthorized if internal-auth fails to authenticate (missing token)" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.create(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(acceptHeader)
      val result:      Future[Result]                      = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result) shouldBe UNAUTHORIZED

      verify(mockHaulierStatusService, never).create(mEq(correlationId))
      verify(mockStubBehaviour, never).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 403 Forbidden if internal-auth fails to authorize access to the resource" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockStubBehaviour.stubAuth(any(), any[Retrieval[Unit]]))
        .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", FORBIDDEN)))
      when(mockHaulierStatusService.create(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val result: Future[Result] = controller.create()(fakeRequest.withBody(Json.toJson(correlationId)))

      status(result) shouldBe FORBIDDEN

      verify(mockHaulierStatusService, never).create(mEq(correlationId))
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }
  }

  "update" should {
    "return 200 Ok after successfully updating if correlation id exists" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.update(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val result: Future[Result] = controller.update(correlationId)(fakeRequest)

      status(result) shouldBe OK

      verify(mockHaulierStatusService).update(mEq(correlationId))
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 404 Not Found if correlation id does not exist" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.update(mEq(correlationId))).thenReturn(EitherT.leftT(CorrelationIdNotFound))

      val result: Future[Result] = controller.update(correlationId)(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include(s"No entry with correlation id ${correlationId.id} found")

      verify(mockHaulierStatusService).update(mEq(correlationId))
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 401 Unauthorized if internal-auth fails to authenticate (expired or invalid token)" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockStubBehaviour.stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval)))
        .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))
      when(mockHaulierStatusService.update(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val result: Future[Result] = controller.update(correlationId)(fakeRequest)

      status(result) shouldBe UNAUTHORIZED

      verify(mockHaulierStatusService, never).update(mEq(correlationId))
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 401 Unauthorized if internal-auth fails to authenticate (missing token)" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockHaulierStatusService.update(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(acceptHeader)
      val result:      Future[Result]                      = controller.update(correlationId)(fakeRequest)

      status(result) shouldBe UNAUTHORIZED

      verify(mockHaulierStatusService, never).update(mEq(correlationId))
      verify(mockStubBehaviour, never).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }

    "return 403 Forbidden if internal-auth fails to authorize access to the resource" in new Setup {
      val correlationId: CorrelationId = CorrelationId("corr-id-1")

      when(mockStubBehaviour.stubAuth(any(), any[Retrieval[Unit]]))
        .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", FORBIDDEN)))
      when(mockHaulierStatusService.update(mEq(correlationId))).thenReturn(EitherT.rightT(correlationId.id))

      val result: Future[Result] = controller.update(correlationId)(fakeRequest)

      status(result) shouldBe FORBIDDEN

      verify(mockHaulierStatusService, never).update(mEq(correlationId))
      verify(mockStubBehaviour).stubAuth(mEq(Some(expectedPermissionPredicate)), mEq(Retrieval.EmptyRetrieval))
    }
  }
}
