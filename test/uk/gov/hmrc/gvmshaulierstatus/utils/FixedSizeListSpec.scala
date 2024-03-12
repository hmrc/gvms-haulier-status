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
