package net.cardnell.mkver

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MainSpec extends AnyFlatSpec with Matchers {
  "The Hello object" should "say hello" in {
    val regex = "    fix(\\(.+\\))?:"
    "    fix:".matches(regex) shouldEqual true
    "    fix(a component):".matches(regex) shouldEqual true
    "    fix".matches(regex) shouldEqual false
  }
}
