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

package uk.gov.hmrc.gvmshaulierstatus.repositories

import org.mongodb.scala.model.Filters
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId
import uk.gov.hmrc.gvmshaulierstatus.model.documents.HaulierStatusDocument
import uk.gov.hmrc.gvmshaulierstatus.model.documents.Status.{Created, Received}
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps

import java.time.temporal.ChronoUnit.MILLIS
import java.time.{Clock, Instant}
import org.mongodb.scala.ObservableFuture

class HaulierStatusRepositoryRepositorySpec extends BaseRepositorySpec[HaulierStatusDocument] {

  override protected val repository: HaulierStatusRepository = new HaulierStatusRepository(mongoComponent)

  val createdAt       = Instant.now
  val lastUpdatedAt   = Instant.now
  val createdDocument = HaulierStatusDocument("corr-1", Created, createdAt, lastUpdatedAt)

  "findAndUpdate" should {
    "return the id of the record after successfully updating" in {
      await(insert(createdDocument))
      await(repository.findAndUpdate(CorrelationId("corr-1"), Received)) shouldBe Some("corr-1")

      val document = await(repository.collection.find(Filters.equal("id", "corr-1".toBson)).toFuture()).headOption.value
      document.status                                    shouldBe Received
      document.lastUpdatedAt.isAfter(document.createdAt) shouldBe true
    }

    "return None when there is no record with that id" in {
      await(repository.findAndUpdate(CorrelationId("corr-2"), Received)) shouldBe None
    }
  }

  "findOlderThan" should {
    "return only records older than the specified time" in {
      await(insert(createdDocument))
      await(insert(createdDocument.copy(id = "corr-2", createdAt = createdAt.minusSeconds(21))))
      inside(await(repository.findAllOlderThan(20, 50))) { case Seq(document) =>
        document.id shouldBe "corr-2"
      }
    }
  }

  "create" should {
    "return correlation id after inserting new status record" in {
      val instant = Instant.now(Clock.systemUTC()).minusSeconds(5)
      await(repository.create(CorrelationId("corr-5"))(instant)) shouldBe "corr-5"

      inside(await(repository.collection.find(Filters.equal("id", "corr-5".toBson)).toFuture())) { case Seq(document) =>
        document shouldBe HaulierStatusDocument("corr-5", Created, instant.truncatedTo(MILLIS), instant.truncatedTo(MILLIS))
      }
    }
  }
}
