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

import uk.gov.hmrc.gvmshaulierstatus.helpers.BaseSpec

class FixedSizeListSpec extends BaseSpec {

  "add" should {
    "add single element" in {
      val fixedSizeList = new FixedSizeList[String](3)

      fixedSizeList.add("one")

      fixedSizeList.getAll shouldBe List("one")
    }

    "add elements in order up to max size without replacing/ejecting" in {
      val fixedSizeList = new FixedSizeList[String](3)

      fixedSizeList.add("one")
      fixedSizeList.add("two")
      fixedSizeList.add("three")

      fixedSizeList.getAll shouldBe List("one", "two", "three")
    }

    "eject the oldest element when already at max size" in {
      val fixedSizeList = new FixedSizeList[String](3)

      fixedSizeList.add("one")
      fixedSizeList.add("two")
      fixedSizeList.add("three")
      fixedSizeList.add("four")

      fixedSizeList.getAll shouldBe List("two", "three", "four")
    }
  }
}
