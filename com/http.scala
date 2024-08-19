//> using dep com.softwaremill.sttp.tapir::tapir-core:1.11.0
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-cats:1.11.0
//> using dep com.softwaremill.sttp.tapir::tapir-json-circe:1.11.1
//> using dep com.github.fd4s::fs2-kafka:3.5.1
//> using dep org.typelevel::log4cats-slf4j:2.7.0
//> using dep ch.qos.logback:logback-classic:1.5.6
//> using dep is.cir::ciris:3.6.0
//> using publish.repository github:bthuillier/small-libs

package com.gilwath.http

import sttp.tapir.server.netty.cats.NettyCatsServer
import cats.effect.*
import cats.syntax.all.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

final case class HttpConfig(host: String, port: Int)

def buildHttpServer(
    httpConfig: HttpConfig
)(f: NettyCatsServer[IO] => NettyCatsServer[IO]) =
  NettyCatsServer
    .io()
    .flatMap { server =>
      Resource.make {
        val transformedServer =
          f(server.port(httpConfig.port).host(httpConfig.host))
        for
          logger <- Slf4jLogger.create[IO]
          _ <- logger.info(
            s"Starting server on ${httpConfig.host}:${httpConfig.port}"
          )
          binding <- transformedServer.start().onError { case e =>
            logger.error(e)("Error starting server")
          }
          _ <- logger.info(
            s"Server started on ${httpConfig.host}:${httpConfig.port}"
          )
        yield binding
      }(_.stop())
    }