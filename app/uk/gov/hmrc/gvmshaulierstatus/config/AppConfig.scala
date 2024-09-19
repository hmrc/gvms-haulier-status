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

package uk.gov.hmrc.gvmshaulierstatus.config

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val config: Configuration) {

  val expireAfterSeconds:       Long   = config.get[Long]("mongodb.haulier-status.expireAfterSeconds")
  val initialDelaySeconds:      Int    = config.get[Int]("statusCheckScheduler.initialDelaySeconds")
  val intervalSeconds:          Int    = config.get[Int]("statusCheckScheduler.intervalSeconds")
  val limit:                    Int    = config.get[Int]("statusCheckScheduler.correlationIdsLimit")
  val orangeThreshold:          Double = config.get[Double]("statusCheckScheduler.orangeThreshold")
  val redThreshold:             Double = config.get[Double]("statusCheckScheduler.redThreshold")
  val receivedPercentagesLimit: Int    = config.get[Int]("statusCheckScheduler.receivedPercentagesLimit")

  val haulierServiceId: String = config.get[String]("service.id")

}
