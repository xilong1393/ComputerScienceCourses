class Stats {
  var messages: Int = 0
  var allocated: Int = 0
  var checks: Int = 0
  var touches: Int = 0
  var misses: Int = 0
  var errors: Int = 0
  var leave: Int=0
  var join: Int=0
  var multicast: Int=0
  var receivedMulticast: Int=0
  var groupID:BigInt=BigInt(0)
  def += (right: Stats): Stats = {
    messages += right.messages
    allocated += right.allocated
    checks += right.checks
    touches += right.touches
    misses += right.misses
    errors += right.errors
    leave += right.leave
    join += right.join
    multicast += right.multicast
    receivedMulticast += right.receivedMulticast
    groupID += right.groupID
    this
  }

  override def toString(): String = {
    s"Stats msgs=$messages alloc=$allocated checks=$checks " +
      s"touches=$touches misses=$misses err=$errors " +
      s"leave=$leave join=$join " +
      s"multicast=$multicast receivedMulticast=$receivedMulticast"
  }
}
