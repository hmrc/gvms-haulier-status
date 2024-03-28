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
import uk.gov.hmrc.gvmshaulierstatus.model.State.{AVAILABLE, UNAVAILABLE}
import uk.gov.hmrc.gvmshaulierstatus.model.documents.HaulierStatusDocument
import uk.gov.hmrc.gvmshaulierstatus.model.documents.Status.{Created, Received}
import uk.gov.hmrc.gvmshaulierstatus.model.{CorrelationId, State}
import uk.gov.hmrc.gvmshaulierstatus.repositories.HaulierStatusRepository
import uk.gov.hmrc.gvmshaulierstatus.utils.FixedSizeList
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HaulierStatusService @Inject() (
  haulierStatusRepository:       HaulierStatusRepository,
  customsServiceStatusConnector: CustomsServiceStatusConnector
)(implicit
  ec:        ExecutionContext,
  appConfig: AppConfig
) extends Logging {

  private val receivedPercentages = new FixedSizeList[Double](appConfig.receivedPercentagesLimit)
  private var currentState: State = AVAILABLE

  initialise()

  private def initialise(): Future[Unit] = {
    logger.info("Initialising state...")
    setState(AVAILABLE)(HeaderCarrier())
  }

  def create(correlationId: CorrelationId): EitherT[Future, CreateHaulierStatusError, String] =
    EitherT(
      haulierStatusRepository
        .create(correlationId)
        .map(_.asRight[CreateHaulierStatusError])
        .recover { case _: MongoWriteException =>
          Left(CorrelationIdAlreadyExists)
        }
    )

  def update(correlationId: CorrelationId): EitherT[Future, DeleteHaulierStatusError, String] =
    EitherT.fromOptionF(haulierStatusRepository.findAndUpdate(correlationId, Received), CorrelationIdNotFound)

  def updateStatus()(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    haulierStatusRepository.findAllOlderThan(appConfig.intervalSeconds, appConfig.limit).flatMap { documents =>
      val receivedDocsPercentage = if (documents.nonEmpty) (documents.count(_.status == Received).toFloat / documents.size) * 100 else 0
      receivedPercentages.add(receivedDocsPercentage)
      logger.debug(s"% of documents with 'Received' status: ${String.format("%.2f", receivedDocsPercentage)}")

      if (documents.isEmpty || (currentState == UNAVAILABLE && receivedPercentages.forAllAndFull(_ >= appConfig.orangeThreshold))) {
        setState(AVAILABLE)
      } else if (
        (currentState == AVAILABLE && receivedDocsPercentage < appConfig.redThreshold) || (currentState == AVAILABLE && receivedPercentages
          .forAllAndFull(_ < appConfig.orangeThreshold))
      ) {
        logMissingReceipts(documents)
        setState(UNAVAILABLE)
      } else {
        Future.unit
      }
    }

  private def logMissingReceipts(documents: Seq[HaulierStatusDocument]): Unit = {
    val createdDocs = documents.filter(_.status == Created)
    logger.info(
      s"${createdDocs.length} documents found with 'Created' status, (curtailed): ${createdDocs.takeRight(10).map(_.toString).mkString("\n", "\n", "")}"
    )
  }

  private def setState(state: State)(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    logger.info(s"Setting haulier status to ${state.value}")
    currentState = state
    customsServiceStatusConnector.updateStatus(appConfig.haulierServiceId, state).map(_ => ())
  }

  // should only be called from Test
  private[services] def overrideState(state: State): Unit =
    currentState = state
}
