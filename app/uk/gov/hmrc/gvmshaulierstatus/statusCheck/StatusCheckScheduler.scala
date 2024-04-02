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

package uk.gov.hmrc.gvmshaulierstatus.statusCheck

import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import uk.gov.hmrc.gvmshaulierstatus.config.AppConfig
import uk.gov.hmrc.gvmshaulierstatus.services.HaulierStatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.lock.{MongoLockRepository, ScheduledLockService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

@Singleton
class StatusCheckScheduler @Inject() (
  actorSystem:          ActorSystem,
  haulierStatusService: HaulierStatusService,
  mongoLockRepository:  MongoLockRepository,
  timestampSupport:     TimestampSupport
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends Logging {

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val lockId              = "status_check_lock"
  private val initialDelaySeconds = appConfig.initialDelaySeconds.seconds
  private val intervalSeconds     = appConfig.intervalSeconds.seconds

  private val lockService =
    ScheduledLockService(
      lockRepository = mongoLockRepository,
      lockId = lockId,
      timestampSupport = timestampSupport,
      schedulerInterval = intervalSeconds
    )

  haulierStatusService.initialise()

  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = initialDelaySeconds,
    interval = intervalSeconds
  ) { () =>
    lockService
      .withLock {
        haulierStatusService.updateStatus()
      }
      .map {
        case Some(_) => logger.debug("Finished StatusCheck. Lock has been released.")
        case None    => logger.debug("Failed to take StatusCheck lock.")
      }
  }
}
