package ch.epfl.ts.first

import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.Order


trait Persistance[T] {
  def save(t: T)
  def save(ts: List[T])
  
  def loadSingle(id: Int) : T
  def loadBatch(startTime: Long, endTime: Long) : List[T]
}