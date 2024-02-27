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

import com.mongodb.client.model.Indexes.{ascending, descending}
import org.bson.codecs.Codec
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.model.Aggregates.sort
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import uk.gov.hmrc.gvmshaulierstatus.config.AppConfig
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId
import uk.gov.hmrc.gvmshaulierstatus.model.documents.HaulierStatusDocument
import uk.gov.hmrc.gvmshaulierstatus.model.documents.Status.Created
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs._
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.play.http.logging.Mdc

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
@Singleton
class HaulierStatusRepository @Inject()(
  mongo:                     MongoComponent,
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends PlayMongoRepository[HaulierStatusDocument](
      collectionName = "haulier-status",
      mongoComponent = mongo,
      domainFormat   = HaulierStatusDocument.mongoFormat,
      indexes = Seq(
        IndexModel(ascending("id"), IndexOptions().name("correlationId").unique(true).sparse(true)),
        IndexModel(
          ascending("createdAt"),
          IndexOptions().name("haulier_status_createdAt").expireAfter(appConfig.expireAfterSeconds, SECONDS).sparse(false)),
      ),
      extraCodecs = Seq[Codec[_]](
        Codecs.playFormatCodec[HaulierStatusDocument](HaulierStatusDocument.mongoFormat)
      ),
      replaceIndexes = true
    ) {

  def findAllOlderThan(seconds: Int, limit: Int): Future[Seq[HaulierStatusDocument]] =
    Mdc.preservingMdc(
      collection
        .find(lt("createdAt", BsonDateTime(Instant.now(Clock.systemUTC()).minusSeconds(seconds).toEpochMilli)))
        .sort(descending("createdAt"))
        .limit(limit)
        .toFuture()
    )

  def findAndDelete(correlationId: CorrelationId): Future[Option[String]] =
    Mdc.preservingMdc(
      collection
        .findOneAndDelete(equal("id", correlationId.id.toBson()))
        .toFutureOption()
        .map(_.map(_.id))
    )

  def create(correlationId: CorrelationId)(implicit instant: Instant = Instant.now(Clock.systemUTC())): Future[String] =
    Mdc.preservingMdc(
      collection
        .insertOne(HaulierStatusDocument(correlationId.id, Created, instant, instant))
        .toFuture()
        .map(_ => correlationId.id)
    )

}
