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

import org.mockito.ArgumentMatchers.{eq => mEq}
import org.mockito.Mockito.when
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{MongoWriteException, ServerAddress, WriteError}
import uk.gov.hmrc.gvmshaulierstatus.error.HaulierStatusError.{CorrelationIdAlreadyExists, CorrelationIdNotFound}
import uk.gov.hmrc.gvmshaulierstatus.helpers.BaseSpec
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId

import scala.concurrent.Future

class HaulierStatusServiceSpec extends BaseSpec {

  trait Setup {
    val service = new HaulierStatusService(mockHaulierStatusRepository)
  }

  "create" should {
    "successfully create if correlation id does not already exist" in new Setup {
      val correlationId = CorrelationId("new-correlation-id")

      when(mockHaulierStatusRepository.create(mEq(correlationId))).thenReturn(Future.successful(correlationId.id))

      service.create(correlationId).value.futureValue shouldBe Right(correlationId.id)
    }

    "return correlation id already exists if there is an existing entry with that id" in new Setup {
      val correlationId = CorrelationId("pre-existing-correlation-id")

      when(mockHaulierStatusRepository.create(mEq(correlationId)))
        .thenReturn(Future.failed(new MongoWriteException(new WriteError(-1, "", new BsonDocument()), new ServerAddress)))

      service.create(correlationId).value.futureValue shouldBe Left(CorrelationIdAlreadyExists)
    }
  }

  "delete" should {
    "successfully delete if correlation id exists" in new Setup {
      val correlationId = CorrelationId("pre-existing-correlation-id")

      when(mockHaulierStatusRepository.findAndDelete(correlationId))
        .thenReturn(Future.successful(Some(correlationId.id)))

      service.delete(correlationId).value.futureValue shouldBe Right(correlationId.id)
    }

    "return correlation id not found if no entry with that id" in new Setup {
      val correlationId = CorrelationId("non-existent-correlation-id")

      when(mockHaulierStatusRepository.findAndDelete(correlationId))
        .thenReturn(Future.successful(None))

      service.delete(correlationId).value.futureValue shouldBe Left(CorrelationIdNotFound)
    }
  }
}
