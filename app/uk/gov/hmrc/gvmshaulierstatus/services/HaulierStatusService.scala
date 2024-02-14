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

import cats.data.EitherT
import cats.implicits._
import org.mongodb.scala.MongoWriteException
import play.api.Logging
import uk.gov.hmrc.gvmshaulierstatus.config.AppConfig
import uk.gov.hmrc.gvmshaulierstatus.connectors.CustomsServiceStatusConnector
import uk.gov.hmrc.gvmshaulierstatus.error.HaulierStatusError.{CorrelationIdAlreadyExists, CorrelationIdNotFound, CreateHaulierStatusError, DeleteHaulierStatusError}
import uk.gov.hmrc.gvmshaulierstatus.model.CorrelationId
import uk.gov.hmrc.gvmshaulierstatus.model.State.{AVAILABLE, UNAVAILABLE}
import uk.gov.hmrc.gvmshaulierstatus.repositories.HaulierStatusRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HaulierStatusService @Inject()(haulierStatusRepository: HaulierStatusRepository, customsServiceStatusConnector: CustomsServiceStatusConnector)(
  implicit ec:                                                ExecutionContext,
  appConfig:                                                  AppConfig)
    extends Logging {

  def create(correlationId: CorrelationId): EitherT[Future, CreateHaulierStatusError, String] =
    EitherT(
      haulierStatusRepository
        .create(correlationId)
        .map(_.asRight[CreateHaulierStatusError])
        .recover {
          case _: MongoWriteException => Left(CorrelationIdAlreadyExists)
        })

  def delete(correlationId: CorrelationId): EitherT[Future, DeleteHaulierStatusError, String] =
    EitherT.fromOptionF(haulierStatusRepository.findAndDelete(correlationId), CorrelationIdNotFound)

  def updateStatus()(implicit headerCarrier: HeaderCarrier): Future[Future[HttpResponse]] =
    haulierStatusRepository.findAllOlderThan(appConfig.notificationCheckSchedulerIntervalSeconds).map {
      case Nil =>
        logger.info("Setting haulier status to AVAILABLE")
        customsServiceStatusConnector.updateStatus(appConfig.haulierServiceId, AVAILABLE)
      case correlationIds =>
        logger.warn("Setting haulier status to UNAVAILABLE")
        logger.info(s"${correlationIds.length} Correlation ids found (curtailed): ${correlationIds.takeRight(10).mkString(", ")}")
        customsServiceStatusConnector.updateStatus(appConfig.haulierServiceId, UNAVAILABLE)
    }

}
