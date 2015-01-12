package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Transaction

class Arbitrageur(uid: Long, firstMid: Long, secondMid: Long) extends Component {

  var firstTradingPrice: Double = 0.0
  var secondTradingPrice: Double = 0.0

  def receiver = {
    case t: Transaction => {
      println("Arbitrageur: received transaction: " + t)
      t.mid match {
        case `firstMid`  => firstTradingPrice = t.price
        case `secondMid` => secondTradingPrice = t.price
        case _ => println("Arbitrageur: unknown MarketId")
      }
      println("Arbitrageur: firstP=" + firstTradingPrice + ", secondP=" + secondTradingPrice + ". diff=" + (firstTradingPrice-secondTradingPrice)/secondTradingPrice)
    }
    case _ => println("Arbitrageur: received unknown")
  }
}