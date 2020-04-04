package net.cardnell.mkver

import java.time.LocalDate

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
      Format("c", "{a}.{b}"),
      Format("r1", "{r2}")
    ))
    formatter.format("hello") should be("hello")
    formatter.format("{a}") should be("1")
    formatter.format("{a}-{c}") should be("1-1.2")
    formatter.format("{r1}") should be("{r2}")
    formatter.format("{a3}") should be("3")
  }

  "branchNameToVariable" should "replace variables" in {
    Formatter.branchNameToVariable("refs/heads/feat/f1") should be("feat_f1")
  }

  "Formatter" should "default list" in {
    val versionData = VersionData(1,2,3,4,"feature/f1", "abcd", "abcdefg", LocalDate.now(), "56")
    val branchConfig = BranchConfig(".*", "v", true, "Version", "release {Version}", "RC", Nil, Nil)
    val formatter = Formatter(versionData, branchConfig)
    //formatter.format("{env.HOME}") should be("")
    formatter.format("{x}") should be("1")
    formatter.format("{y}") should be("2")
    formatter.format("{z}") should be("3")
    formatter.format("{br}") should be("feature_f1")
  }
}
