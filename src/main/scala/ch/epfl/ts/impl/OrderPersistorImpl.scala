package ch.epfl.ts.impl

import ch.epfl.ts.first.Persistor
import ch.epfl.ts.data.Order
import java.util.ArrayList

class OrderPersistorImpl extends Persistor[Order] {

  def process(data: Order): Unit = {

  }

  def save(t: Order) = {
    
  }
  def save(ts: List[Order]) = {
    
  }

  def loadSingle(id: Int): Order = {
    
	return new Order(2.0)
  }
  
  def loadBatch(startTime: Long, endTime: Long): List[Order] = {
    return List()
  }

}