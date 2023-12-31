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

package uk.gov.hmrc.gvmshaulierstatus.services

import org.mockito.ArgumentMatchers.{any, anyInt, anyString, same, eq => mEq}
import org.mockito.Mockito.{verify, when}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{MongoWriteException, ServerAddress, WriteError}
import uk.gov.hmrc.gvmshaulierstatus.error.HaulierStatusError.{CorrelationIdAlreadyExists, CorrelationIdNotFound}
import uk.gov.hmrc.gvmshaulierstatus.helpers.BaseSpec
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId
import uk.gov.hmrc.gvmshaulierstatus.model.State.{AVAILABLE, UNAVAILABLE}
import uk.gov.hmrc.gvmshaulierstatus.model.documents.HaulierStatusDocument
import uk.gov.hmrc.http.HttpResponse

import java.time.Instant
import scala.concurrent.Future

class HaulierStatusServiceSpec extends BaseSpec {

  trait Setup {
    val service = new HaulierStatusService(mockHaulierStatusRepository, mockCustomsServiceStatusConnector)
  }

  "create" should {
    "successfully create if correlation id does not already exist" in new Setup {
      val correlationId: CorrelationId = CorrelationId("new-correlation-id")

      when(mockHaulierStatusRepository.create(correlationId)).thenReturn(Future.successful(correlationId.id))

      service.create(correlationId).value.futureValue shouldBe Right(correlationId.id)
    }

    "return correlation id already exists if there is an existing entry with that id" in new Setup {
      val correlationId: CorrelationId = CorrelationId("pre-existing-correlation-id")

      when(mockHaulierStatusRepository.create(correlationId))
        .thenReturn(Future.failed(new MongoWriteException(new WriteError(-1, "", new BsonDocument()), new ServerAddress)))

      service.create(correlationId).value.futureValue shouldBe Left(CorrelationIdAlreadyExists)
    }
  }

  "delete" should {
    "successfully delete if correlation id exists" in new Setup {
      val correlationId: CorrelationId = CorrelationId("pre-existing-correlation-id")

      when(mockHaulierStatusRepository.findAndDelete(correlationId))
        .thenReturn(Future.successful(Some(correlationId.id)))

      service.delete(correlationId).value.futureValue shouldBe Right(correlationId.id)
    }

    "return correlation id not found if no entry with that id" in new Setup {
      val correlationId: CorrelationId = CorrelationId("non-existent-correlation-id")

      when(mockHaulierStatusRepository.findAndDelete(correlationId))
        .thenReturn(Future.successful(None))

      service.delete(correlationId).value.futureValue shouldBe Left(CorrelationIdNotFound)
    }
  }

  "updateStatus" should {
    "set status to AVAILABLE if no correlation ids older than threshold" in new Setup {
      when(mockHaulierStatusRepository.findAllOlderThan(anyInt()))
        .thenReturn(Future.successful(Seq.empty[HaulierStatusDocument]))

      when(mockCustomsServiceStatusConnector.updateStatus(anyString(), any())(any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      service.updateStatus().futureValue

      verify(mockCustomsServiceStatusConnector).updateStatus(mEq("haulier"), same(AVAILABLE))(any())
    }

    "set status to UNAVAILABLE if there are correlation ids older than threshold" in new Setup {
      when(mockHaulierStatusRepository.findAllOlderThan(anyInt()))
        .thenReturn(Future.successful(Seq(HaulierStatusDocument("some-id", Instant.now.minusSeconds(60)))))

      when(mockCustomsServiceStatusConnector.updateStatus(anyString(), any())(any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      service.updateStatus().futureValue

      verify(mockCustomsServiceStatusConnector).updateStatus(mEq("haulier"), same(UNAVAILABLE))(any())
    }
  }
}
