package ch.epfl.ts.first

/**
 * Defines the Persistance interface
 * @tparam T
 */
trait Persistance[T] {
  def save(t: T)
  def save(ts: List[T])
  def loadSingle(id: Int) : T
  def loadBatch(startTime: Long, endTime: Long) : List[T]
}


