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
import uk.gov.hmrc.gvmshaulierstatus.helpers.BaseISpec
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId
import uk.gov.hmrc.gvmshaulierstatus.model.documents.HaulierStatusDocument
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

class HaulierStatusRepositoryISpec extends BaseISpec {

  val haulierStatusRepository: HaulierStatusRepository = app.injector.instanceOf[HaulierStatusRepository]

  "findAndDelete" should {
    "return the id of the record after successfully deleting" in {
      await(haulierStatusRepository.create(CorrelationId("corr-1"))(Instant.now(Clock.systemUTC())))
      await(haulierStatusRepository.findAndDelete(CorrelationId("corr-1"))) shouldBe Some("corr-1")

      await(haulierStatusRepository.collection.find(Filters.equal("id", "corr-1".toBson())).toFuture()).length shouldBe 0
    }

    "return None when there is no record with that id" in {
      await(haulierStatusRepository.findAndDelete(CorrelationId("corr-1"))) shouldBe None
    }
  }

  "findOlderThan" should {
    "return only records older than the specified time" in {
      await(haulierStatusRepository.create(CorrelationId("corr-1"))(Instant.now(Clock.systemUTC())))
      await(haulierStatusRepository.create(CorrelationId("corr-2"))(Instant.now(Clock.systemUTC()).minusSeconds(21)))
      inside(await(haulierStatusRepository.findAllOlderThan(20))) {
        case Seq(document) => document.id shouldBe "corr-2"
      }
    }
  }

  "create" should {
    "return correlation id after inserting new status record" in {
      val instant = Instant.now(Clock.systemUTC()).minusSeconds(5)
      await(haulierStatusRepository.create(CorrelationId("corr-5"))(instant)) shouldBe "corr-5"

      inside(await(haulierStatusRepository.collection.find(Filters.equal("id", "corr-5".toBson())).toFuture())) {
        case Seq(document) => document shouldBe HaulierStatusDocument("corr-5", instant.truncatedTo(ChronoUnit.MILLIS))
      }
    }
  }
}
