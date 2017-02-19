import java.time.LocalTime
import java.time.format.DateTimeFormatter

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.ServerSentEvent
import io.allquantor.MesosBufferGraphStage

import scala.concurrent.duration._


object Main extends App {

  import Directives._
  import de.heikoseeberger.akkasse.EventStreamMarshalling._

  implicit val system = ActorSystem()

  // That does the trick!.
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val mat = ActorMaterializer()


  private def route = {
    def events =
      path("events") {
        get {
          complete {
            Source
              .tick(1.millis, 1000.millis, NotUsed)
              .map(_ => LocalTime.now())
              .map(timeToServerSentEvent).
              via(new MesosBufferGraphStage[ServerSentEvent](5, dropMessage))
          }
        }
      }

    events
  }

  private def timeToServerSentEvent(time: LocalTime) =
    ServerSentEvent(DateTimeFormatter.ISO_LOCAL_TIME.format(time), "current-time")

  lazy val dropMessage = ServerSentEvent("you lost messages", "drop-messages")

  Http(system).bindAndHandle(route, "localhost", 8080)

}
