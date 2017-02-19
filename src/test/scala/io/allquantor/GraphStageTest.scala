package io.allquantor

import java.time.LocalTime
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import de.heikoseeberger.akkasse.ServerSentEvent
import org.scalatest.{Matchers, WordSpec}


class GraphStageTest extends WordSpec with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()


  "Graph Stage" should {
    "inject bufferoverflowmessage when buffer is full" in {

      // Create test subscriber (client) and test publisher.
      val publisher = TestPublisher.probe[ServerSentEvent]()
      val subscriber = TestSubscriber.manualProbe[ServerSentEvent]()

      // Message should be send when buffer was full.
      val bufferOverflowMessage = ServerSentEvent("you lost messages", "drop-messages")

      def timeToServerSentEvent(time: LocalTime) =
        ServerSentEvent(DateTimeFormatter.ISO_LOCAL_TIME.format(time), "current-time")

      // Customized buffer.
      val bufferSize = 5
      val graphStage = new NotifierGraphStage[ServerSentEvent](bufferSize, bufferOverflowMessage)

      // Test event flow.
      val eventFlow = Source.fromPublisher(publisher).
        via(graphStage).
        to(Sink.fromSubscriber(subscriber))

      eventFlow.run()

      // Get client handle.
      val sub = subscriber.expectSubscription()

      // Fill up buffer
      for (_ ← 1 to bufferSize + 1) publisher.sendNext(timeToServerSentEvent(LocalTime.now()))

      // Trigger the pull loop
      for (_ ← 1 to bufferSize) sub.request(1)

      // buffer should contain the overflow message.
      subscriber.expectNext(bufferOverflowMessage)


      val _msg = timeToServerSentEvent(LocalTime.now())

      // If the client is up again, the normal messages should be delivered.
      publisher.sendNext(_msg)
      sub.request(1)
      subscriber.expectNext(_msg)

    }
  }
}
