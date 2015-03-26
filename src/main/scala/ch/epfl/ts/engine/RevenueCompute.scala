package ch.epfl.ts.engine

import ch.epfl.ts.component.{ Component, StartSignal }
import ch.epfl.ts.data.Transaction
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import ch.epfl.ts.data.Currency

abstract class RevenueCompute(traderNames: Map[Long, String]) extends Component {

 import context._

 //Important TODO : Wallet Component , 1 wallet per trader.
 
  case class Tick()
   case class Wallet(var funds: Map[Currency.Value, Double])

  var wallets = Map[Long, Wallet]()
  var oldTradingPrice: Double = 0.0
  var currentTradingPrice: Double = 0.0


  def process(t: Transaction)
  
 
 //TODO Better stats (when Wallet component is ready)
 def displayStats(traderId: Long) = {
    var disp = new StringBuffer()
    disp.append(f"Stats: trading price=$currentTradingPrice%s \n")
    disp.append("Stats: trader: " + traderNames(traderId) + ", cash=" + wallets(traderId).funds + "\n")
    print(disp)
  }



}