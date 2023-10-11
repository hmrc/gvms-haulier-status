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

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, EitherValues, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait BaseSpec
    extends AnyWordSpec
    with Matchers
    with OptionValues
    with EitherValues
    with DefaultAwaitTimeout
    with MockitoSugar
    with ScalaFutures
    with FutureAwaits
    with BeforeAndAfterEach
    with AllMocks
    with Configs {

  implicit lazy val ec:                 ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val hc:                 HeaderCarrier    = HeaderCarrier()
  implicit lazy val system:             ActorSystem      = ActorSystem()
  implicit lazy val materializer:       Materializer     = Materializer(system)
  override implicit val patienceConfig: PatienceConfig   = PatienceConfig(timeout = Span(1, Seconds), interval = Span(50, Millis))

  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(acceptHeader)
}
