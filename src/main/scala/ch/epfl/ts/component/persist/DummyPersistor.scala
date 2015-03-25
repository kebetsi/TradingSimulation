package ch.epfl.ts.component.persist

import ch.epfl.ts.data.Transaction

/**
* a Persistor that does nothing.
* Created by sygi on 23.03.15.
*/
class DummyPersistor extends Persistance[Transaction]{
  override def save(t: Transaction): Unit = {

  }
  override def loadBatch(startTime: Long, endTime: Long): List[Transaction] = {
    return List()
  }
  override def loadSingle(id: Int): Transaction = {
    return null
  }
  override def save(ts: List[Transaction]): Unit = {
  }
}
