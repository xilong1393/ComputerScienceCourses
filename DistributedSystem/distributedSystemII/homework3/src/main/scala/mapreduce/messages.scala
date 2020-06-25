package mapreduce

case class Msg(title: String, url: String)
case object Flush
case object Done
