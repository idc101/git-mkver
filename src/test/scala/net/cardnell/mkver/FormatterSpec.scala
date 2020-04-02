package net.cardnell.mkver

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.config.read
import zio.config.typesafe.TypeSafeConfigSource

class FormatterSpec extends AnyFlatSpec with Matchers {

  "formatter" should "replace variables" in {
    val formatter = Formatter.Formatter(List(
      Format("a", "1"),
      Format("a3", "3"),
      Format("b", "2"),
      Format("c", "%a.%b"),
      Format("r1", "%r2")
    ))
    formatter.format("hello") should be("hello")
    formatter.format("%a") should be("1")
    formatter.format("%a-%c") should be("1-1.2")
    formatter.format("%r1") should be("%r2")
    formatter.format("%a3") should be("3")
  }
}
