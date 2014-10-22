package ch.epfl.ts.first

import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.Order


trait Persistance[T] {
  
  
  def save(t: Transaction)
  def save(ts: List[Transaction])
  
  def loadTransaction(id: Int) : Transaction
  def loadTransactions(startTime: Long, endTime: Long) : List[Transaction]
  
  def save(o: Order)
  def save(os: List[Order])
  
  def loadOrder(id: Int) : Order
  def loadOrders(startTime: Long, endTime: Long) : List[Order]

}