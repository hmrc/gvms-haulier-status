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

import org.mockito.ArgumentMatchers.{any, anyInt, anyString, eq as mEq, same}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{MongoWriteException, ServerAddress, WriteError}
import uk.gov.hmrc.gvmshaulierstatus.error.HaulierStatusError.{CorrelationIdAlreadyExists, CorrelationIdNotFound}
import uk.gov.hmrc.gvmshaulierstatus.helpers.BaseSpec
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId
import uk.gov.hmrc.gvmshaulierstatus.model.State.{AVAILABLE, UNAVAILABLE}
import uk.gov.hmrc.gvmshaulierstatus.model.documents.HaulierStatusDocument
import uk.gov.hmrc.gvmshaulierstatus.model.documents.Status.{Created, Received}
import uk.gov.hmrc.http.HttpResponse

import java.time.Instant
import java.util.Collections
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
        .thenReturn(Future.failed(new MongoWriteException(new WriteError(-1, "", new BsonDocument()), new ServerAddress, Collections.emptySet)))

      service.create(correlationId).value.futureValue shouldBe Left(CorrelationIdAlreadyExists)
    }
  }

  "update" should {
    "successfully delete if correlation id exists" in new Setup {
      val correlationId: CorrelationId = CorrelationId("pre-existing-correlation-id")

      when(mockHaulierStatusRepository.findAndUpdate(correlationId, Received))
        .thenReturn(Future.successful(Some(correlationId.id)))

      service.update(correlationId).value.futureValue shouldBe Right(correlationId.id)
    }

    "return correlation id not found if no entry with that id" in new Setup {
      val correlationId: CorrelationId = CorrelationId("non-existent-correlation-id")

      when(mockHaulierStatusRepository.findAndUpdate(correlationId, Received))
        .thenReturn(Future.successful(None))

      service.update(correlationId).value.futureValue shouldBe Left(CorrelationIdNotFound)
    }
  }

  "updateStatus" should {
    "set status to AVAILABLE if no correlation ids older than specified interval" in new Setup {
      when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt()))
        .thenReturn(Future.successful(Seq.empty[HaulierStatusDocument]))

      when(mockCustomsServiceStatusConnector.updateStatus(anyString(), any())(any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      service.updateStatus().futureValue

      verify(mockCustomsServiceStatusConnector).updateStatus(mEq("haulier"), same(AVAILABLE))(any())
    }

    "set status to AVAILABLE if state currently UNAVAILABLE and there are n consecutive percentages at ORANGE threshold" in new Setup {
      service.overrideState(UNAVAILABLE)

      val createdAt        = Instant.now.minusSeconds(60)
      val lastUpdatedAt    = Instant.now.minusSeconds(30)
      val createdDocument  = HaulierStatusDocument("some-id", Created, createdAt, lastUpdatedAt)
      val receivedDocument = createdDocument.copy(status = Received)
      val dbResult         = List.fill(18)(receivedDocument) ++ List.fill(2)(createdDocument)

      (1 to 5) foreach { _ =>
        when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))
        service.updateStatus().futureValue
      }

      when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))

      when(mockCustomsServiceStatusConnector.updateStatus(anyString(), any())(any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      service.updateStatus().futureValue

      verify(mockCustomsServiceStatusConnector).updateStatus(mEq("haulier"), same(AVAILABLE))(any())
    }

    "do not change status if state currently UNAVAILABLE and there are n-1 consecutive percentages at ORANGE threshold then 1 below ORANGE" in new Setup {
      service.overrideState(UNAVAILABLE)

      val createdAt        = Instant.now.minusSeconds(60)
      val lastUpdatedAt    = Instant.now.minusSeconds(30)
      val createdDocument  = HaulierStatusDocument("some-id", Created, createdAt, lastUpdatedAt)
      val receivedDocument = createdDocument.copy(status = Received)
      val dbResult         = List.fill(18)(receivedDocument) ++ List.fill(2)(createdDocument)

      (1 to 5) foreach { _ =>
        when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))
        service.updateStatus().futureValue
      }

      when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt()))
        .thenReturn(Future.successful(List.fill(18)(receivedDocument) ++ List.fill(3)(createdDocument)))

      service.updateStatus().futureValue

      verifyNoInteractions(mockCustomsServiceStatusConnector)
    }

    "do not change status if state currently UNAVAILABLE and there is 1 below ORANGE threshold then n-1 consecutive percentages at ORANGE threshold" in new Setup {
      service.overrideState(UNAVAILABLE)

      val createdAt        = Instant.now.minusSeconds(60)
      val lastUpdatedAt    = Instant.now.minusSeconds(30)
      val createdDocument  = HaulierStatusDocument("some-id", Created, createdAt, lastUpdatedAt)
      val receivedDocument = createdDocument.copy(status = Received)
      val dbResult         = List.fill(18)(receivedDocument) ++ List.fill(2)(createdDocument)

      when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt()))
        .thenReturn(Future.successful(List.fill(18)(receivedDocument) ++ List.fill(3)(createdDocument)))

      service.updateStatus().futureValue

      (1 to 5) foreach { _ =>
        when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))
        service.updateStatus().futureValue
      }

      verifyNoInteractions(mockCustomsServiceStatusConnector)
    }

    "do not change status if state currently AVAILABLE and less than n percentages received even if all below ORANGE threshold" in new Setup {
      val createdAt        = Instant.now.minusSeconds(60)
      val lastUpdatedAt    = Instant.now.minusSeconds(30)
      val createdDocument  = HaulierStatusDocument("some-id", Created, createdAt, lastUpdatedAt)
      val receivedDocument = createdDocument.copy(status = Received)
      val dbResult         = List.fill(18)(receivedDocument) ++ List.fill(3)(createdDocument)

      (1 to 5) foreach { _ =>
        when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))
        service.updateStatus().futureValue
      }

      verifyNoInteractions(mockCustomsServiceStatusConnector)
    }

    "do not change status if state currently AVAILABLE and n-1 consecutive percentages received below ORANGE threshold" in new Setup {
      val createdAt        = Instant.now.minusSeconds(60)
      val lastUpdatedAt    = Instant.now.minusSeconds(30)
      val createdDocument  = HaulierStatusDocument("some-id", Created, createdAt, lastUpdatedAt)
      val receivedDocument = createdDocument.copy(status = Received)
      val dbResult         = List.fill(18)(receivedDocument) ++ List.fill(3)(createdDocument)

      when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt()))
        .thenReturn(Future.successful(List.fill(18)(receivedDocument) ++ List.fill(2)(createdDocument)))

      service.updateStatus().futureValue

      (1 to 5) foreach { _ =>
        when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))
        service.updateStatus().futureValue
      }

      verifyNoInteractions(mockCustomsServiceStatusConnector)
    }

    "set status to UNAVAILABLE if n consecutive percentages received below ORANGE threshold" in new Setup {
      val createdAt        = Instant.now.minusSeconds(60)
      val lastUpdatedAt    = Instant.now.minusSeconds(30)
      val createdDocument  = HaulierStatusDocument("some-id", Created, createdAt, lastUpdatedAt)
      val receivedDocument = createdDocument.copy(status = Received)
      val dbResult         = List.fill(18)(receivedDocument) ++ List.fill(3)(createdDocument)

      (1 to 5) foreach { _ =>
        when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))
        service.updateStatus().futureValue
      }

      when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))

      when(mockCustomsServiceStatusConnector.updateStatus(anyString(), any())(any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      service.updateStatus().futureValue

      verify(mockCustomsServiceStatusConnector).updateStatus(mEq("haulier"), same(UNAVAILABLE))(any())
    }

    "set status to UNAVAILABLE immediately if the RED threshold is breached" in new Setup {
      val createdAt        = Instant.now.minusSeconds(60)
      val lastUpdatedAt    = Instant.now.minusSeconds(30)
      val createdDocument  = HaulierStatusDocument("some-id", Created, createdAt, lastUpdatedAt)
      val receivedDocument = createdDocument.copy(status = Received)
      val dbResult         = List.fill(2)(receivedDocument) ++ List.fill(19)(createdDocument)
      when(mockHaulierStatusRepository.findAllOlderThan(anyInt(), anyInt())).thenReturn(Future.successful(dbResult))

      when(mockCustomsServiceStatusConnector.updateStatus(anyString(), any())(any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      service.updateStatus().futureValue

      verify(mockCustomsServiceStatusConnector).updateStatus(mEq("haulier"), same(UNAVAILABLE))(any())
    }
  }
}
