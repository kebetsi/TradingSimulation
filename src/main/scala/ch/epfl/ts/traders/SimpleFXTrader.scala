package ch.epfl.ts.traders

import ch.epfl.ts.component.Component 
import ch.epfl.ts.indicators.MA
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{MarketAskOrder, MarketBidOrder, Quote}

/* This simple trader will use two moving average and send order when this two MA cross each other.
 * @ Param the length of the two moving average period.
 */
class SimpleFXTrader(val uid: Long, val shortPeriod: Int, val longPeriod : Int, val volume : Double) extends Component{
  
  //this variables are used to synchronized the two moving average 
  var shortMaCount : Int = 0
  var longMaCount : Int = 0
  
  //contains the moving average obtained one period before ( used to detect the point when the two MA cross )
  var previousShort : Double = 0.0
  var previousLong : Double = 0.0
  
  // stock the current Moving average
  var currentShort : Double = 0.0
  var currentLong : Double = 0.0
  
  //TODO what is a good initialization oid
  var oid = 12345
  
  //current ask and bid price
  var askPrice : Double = 0.0
  var bidPrice : Double = 0.0
  
  //to make sure that we have initialized our price before starting to trade
  var priceReady : Boolean = false
  
  override def receiver = {
   
    case ma : MA => {
      println("SimpleFXTrader receided a moving average: " + ma)
      ma.period match {
        case `shortPeriod` => {
          println("received a short period")
          previousShort = currentShort
          currentShort = ma.value
          shortMaCount += 1
        }
        case`longPeriod` => {
          println("received a long period")
          previousLong = currentLong
          currentLong = ma.value
          longMaCount += 1 
        }

        case _ => println( "SimpleFXTrader: received unknown message" )

      }
        if(shortMaCount == longMaCount && priceReady) { //we make sure that we are comparing MA from the same period
            if (previousShort < previousLong && currentShort >= currentLong) {  //if short goes above long buy
              send(MarketBidOrder(oid, uid,System.currentTimeMillis(), USD, CHF, volume, bidPrice))
              println("simple trader : buying")
              oid += 1
            }
            
            else if(previousShort > previousLong && currentShort <= currentLong) { //if short goes below long sell
              send(MarketAskOrder(oid, uid, System.currentTimeMillis(), USD, CHF, volume, askPrice))
              println("simple trader : selling")
              oid += 1
            }
          }   
    }
    case q : Quote => {

      println("SimpleFXTrader receided a quote: " + q)

      if(!priceReady) {
        priceReady =true
      }
      askPrice = q.ask
      bidPrice = q.bid
    }
    case _ => println("SimpleTrader: received unknown")
  }
}