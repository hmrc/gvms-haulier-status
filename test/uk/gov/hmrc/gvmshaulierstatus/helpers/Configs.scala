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

package uk.gov.hmrc.gvmshaulierstatus.helpers

import com.typesafe.config.ConfigFactory
import play.api.{Configuration, Environment}
import uk.gov.hmrc.gvmshaulierstatus.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait Configs {

  def configuration: Configuration = Configuration(ConfigFactory.parseResources("application.conf"))

  def environment: Environment = Environment.simple()

  def servicesConfig = new ServicesConfig(configuration)

  implicit def applicationConfig: AppConfig = new AppConfig(configuration)
}
