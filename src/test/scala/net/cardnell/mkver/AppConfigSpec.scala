package net.cardnell.mkver

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.config.read
import zio.config.typesafe.TypeSafeConfigSource
import zio.config.typesafe.TypeSafeConfigSource.fromHoconString
import zio.{App, Has, ZEnv, ZIO, ZLayer, console}

class AppConfigSpec extends AnyFlatSpec with Matchers {

  "config" should "load" in {
    //val c = TypeSafeConfigSource.fromDefaultLoader
    val c = TypeSafeConfigSource.fromTypesafeConfig(ConfigFactory.load("application.conf"))
    println(c)
    val config =
      c match {
        case Left(value) => Left(value)
        case Right(source) => read(AppConfig.appConfigDesc from source)
      }

    println(config)

    assert(config != null)
  }

}
