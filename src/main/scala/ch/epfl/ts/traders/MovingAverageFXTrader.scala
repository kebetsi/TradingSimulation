package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency.CHF
import ch.epfl.ts.data.Currency.USD
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.indicators.MA

/**
 * This simple trader will use two moving average and send order when this two average cross each other.
 * @param the length of the two moving average period.
 */
class MovingAverageFXTrader(val uid: Long, val shortPeriod: Int, val longPeriod : Int, val volume : Double) extends Component{
  
  var shortMaCount : Int = 0
  var longMaCount : Int = 0
  
  var previousShort : Double = 0.0
  var previousLong : Double = 0.0
  var currentShort : Double = 0.0
  var currentLong : Double = 0.0
  
  // TODO: should the trader be responsible for its own order id?
  var oid = 12345
  
  override def receiver = {
   
    case ma : MA => {
      
      println("SimpleFXTrader receided a moving average : " + ma)
      
      ma.period match {
        case `shortPeriod` => {
          println("received a short period")
          previousShort = currentShort
          currentShort = ma.value
          shortMaCount += 1
        }
        case `longPeriod` => {
          println("received a long period")
          previousLong = currentLong
          currentLong = ma.value
          longMaCount += 1 
        }
        case _ => 
      }
      
      if(shortMaCount == longMaCount) {
        if (previousShort < previousLong && currentShort >= currentLong) {
          // TODO: price should not be 0
          send(MarketBidOrder(oid, uid,System.currentTimeMillis(), USD, CHF, volume, 0))
          println("simple trader : buying")
          oid += 1
        }
        
        else if(previousShort > previousLong && currentShort <= currentLong) {
          // TODO: price should not be 0
          send(MarketAskOrder(oid, uid, System.currentTimeMillis(), USD, CHF, volume, 0))
          println("simple trader : selling")
          oid += 1
        }
      }   
    }
    
    case _ => println("SimpleTrader: received unknown")
    
  }
}