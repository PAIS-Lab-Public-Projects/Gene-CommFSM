/*
*  MIT License
*  Copyright (c) 2017 PAIS-Lab-Public-Projects
*
*  Permission is hereby granted, free of charge, to any person obtaining a copy
*  of this software and associated documentation files (the "Software"), to deal
*  in the Software without restriction, including without limitation the rights
*  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
*  copies of the Software, and to permit persons to whom the Software is
*  furnished to do so, subject to the following conditions:
*
*  The above copyright notice and this permission notice shall be included in all
*  copies or substantial portions of the Software.
*
*  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
*  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
*  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
*  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
*  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
*  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
*  SOFTWARE.
*/

package cfsm.engine

import java.io.File
import java.util.concurrent.atomic.AtomicLong

import cfsm.domain.CFSMConfiguration
import cfsm.engine.Loggers.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Mining {
  /**
    * Entry point of mining
    */
  def begin(conf: CFSMConfiguration,
            destination: String,
            csv: Boolean,
            caseId: AtomicLong,
            eventId: AtomicLong,
            logShowOptions: CmdOptions): Unit = {

    // choosing a logger
    val fileLogger: Option[FileLogger] =
      if (destination == null) {
        None
      } else {
        val file = new File(destination)
        val log = FileLogger(file)

        val logThread = new Thread(log)
        logThread.setDaemon(true)
        logThread.setName("cfsm-logger-01")
        logThread.start()
        Some(log)
      }

    Await.ready(
      Future.sequence(
        (1 to logShowOptions.cases.toInt).map {
          _ =>
            Future {
              val log: Logger = fileLogger match {
                case None => Loggers.SimpleLogger
                case Some(fLogger) => if (csv) Loggers.CSVLogger(fLogger, ";", caseId, eventId, logShowOptions) else Loggers.SimpleFileLogger(fLogger)
              }
              emulate(conf, log, Selectors.RandomSelector, logShowOptions.maxEvents)
            }
        }),
      Int.MaxValue.seconds
    )

    fileLogger match {
      case None =>
      case Some(logger) => logger.close()
    }
  }
}
