/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.gvmshaulierstatus.utils

import scala.collection.mutable.ListBuffer

class FixedSizeList[A](maxSize: Int) {
  private val innerBuffer = new ListBuffer[A]

  def add(element: A): Unit = {
    innerBuffer += element
    while (innerBuffer.length > maxSize) innerBuffer.remove(0)
  }

  def isEmpty: Boolean =
    innerBuffer.isEmpty

  def forAllAndFull(predicate: A => Boolean): Boolean =
    innerBuffer.length == maxSize && innerBuffer.forall(predicate)

  private[utils] def getAll: List[A] =
    innerBuffer.toList
}
