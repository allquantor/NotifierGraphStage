package io.allquantor

import akka.event.Logging
import akka.stream._
import akka.stream.stage._

import scala.collection.mutable


final case class MesosBufferGraphStage[T](size: Int, bufferOverflowMessage: T) extends GraphStage[FlowShape[T, T]] {

  val in = Inlet[T](Logging.simpleName(this) + ".in")
  val out = Outlet[T](Logging.simpleName(this) + ".out")

  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      private var buffer: mutable.Queue[T] = _

      override def preStart(): Unit = {
        buffer = mutable.Queue.empty[T]
        pull(in)
      }

      override def onPush(): Unit = {
        val elem = grab(in)
        // If out is available, then it has been pulled but no dequeued element has been delivered.
        // It means the buffer at this moment is definitely empty,
        // so we just push the current element to out, then pull.
        if (isAvailable(out)) {
          push(out, elem)
          pull(in)
        } else {
          enqueueAction(elem)
        }
      }

      def enqueueAction(elem: T): Unit = {
        if (buffer.size == size) {
          buffer.clear()
          buffer.enqueue(bufferOverflowMessage)
          // We do not pull, since the client
          //pull(in)
        } else if (buffer.nonEmpty && buffer.head == bufferOverflowMessage) {
          // Ignore
        } else {
          buffer.enqueue(elem)
          pull(in)
        }
      }

      override def onPull(): Unit = {
        if (buffer.nonEmpty) {
          push(out, buffer.dequeue())
        }
        if (isClosed(in)) {
          if (buffer.isEmpty) completeStage()
        } else if (!hasBeenPulled(in)) {
          pull(in)
        }
      }

      override def onUpstreamFinish(): Unit = {
        if (buffer.isEmpty) completeStage()
      }

      setHandler(in, this)
      setHandler(out, this)
    }

}

