package token0;

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

case class Neighbors(left:ActorRef, right:ActorRef)
case object PING // goes clockwise
case object PONG // goes counterclockwise

class PingPong extends Actor {
  var left = self
  var right = self
  var PINGcount = 0
  var PONGcount = 0

  def receive = {

    case PING =>
      PINGcount = PINGcount + 1
      printf("%12s : PING=%d, PONG=%d\n", self.path.toStringWithoutAddress, PINGcount, PONGcount)
      Thread sleep 1000
      left ! PING

    case PONG =>
      PONGcount = PONGcount + 1
      printf("%12s : PING=%d, PONG=%d\n", self.path.toStringWithoutAddress, PINGcount, PONGcount)
      Thread sleep 1000
      right ! PONG

    case Neighbors(l, r) =>
      left = l
      right = r
  }
}

object Server extends App {
  val system = ActorSystem("PingPong")
  val first = system.actorOf(Props[PingPong], name = "first")
  val second = system.actorOf(Props[PingPong], name = "second")
  val third = system.actorOf(Props[PingPong], name = "third")
  first ! Neighbors(second, third)
  second ! Neighbors(third, first)
  third ! Neighbors(first, second)
  first ! PING
  first ! PONG
  Thread sleep 10000
  system.terminate
}
