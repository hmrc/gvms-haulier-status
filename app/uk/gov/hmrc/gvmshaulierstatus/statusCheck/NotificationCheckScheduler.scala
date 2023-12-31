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

import akka.actor.ActorSystem
import uk.gov.hmrc.gvmshaulierstatus.config.AppConfig
import uk.gov.hmrc.gvmshaulierstatus.services.HaulierStatusService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

@Singleton
class NotificationCheckScheduler @Inject()(
  actorSystem:               ActorSystem,
  haulierStatusService:      HaulierStatusService
)(implicit executionContext: ExecutionContext, appConfig: AppConfig) {

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier()

  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = appConfig.notificationCheckSchedulerInitialDelaySeconds seconds,
    interval     = appConfig.notificationCheckSchedulerIntervalSeconds seconds
  ) { () =>
    haulierStatusService.updateStatus()
  }

}
