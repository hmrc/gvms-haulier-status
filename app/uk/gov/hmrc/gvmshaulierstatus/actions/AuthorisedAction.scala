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

package uk.gov.hmrc.gvmshaulierstatus.actions

import javax.inject.{Inject, Singleton}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Results}
import uk.gov.hmrc.internalauth.client._

import scala.concurrent.Future

@Singleton
class AuthorisedAction @Inject() (val internalAuth: BackendAuthComponents) {

  private val writePermission: Predicate.Permission = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType("gvms-haulier-status"),
      resourceLocation = ResourceLocation("*")
    ),
    action = IAAction("WRITE")
  )

  def withInternalAuth: ActionBuilder[Request, AnyContent] =
    internalAuth.authorizedAction(
      writePermission,
      onUnauthorizedError = Future.successful(Results.Unauthorized("Unauthorized request")),
      onForbiddenError = Future.successful(Results.Forbidden("Forbidden request"))
    )
}
