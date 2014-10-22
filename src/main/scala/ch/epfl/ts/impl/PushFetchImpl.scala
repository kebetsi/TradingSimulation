package ch.epfl.ts.impl

import ch.epfl.ts.first.PushFetch
import ch.epfl.ts.data.Transaction

class PushFetchImpl extends PushFetch[Transaction] {
  
  var send: Transaction => Unit
  
  override def setCallback (f: Transaction => Unit): Unit = {
    send = f
  }
}