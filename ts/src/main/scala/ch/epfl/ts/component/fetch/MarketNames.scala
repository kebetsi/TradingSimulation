package ch.epfl.ts.component.fetch

object MarketNames {
  val BTCE_NAME = "BTC-e"
  val BTCE_ID = 1L
  val BITSTAMP_NAME = "Bitstamp"
  val BITSTAMP_ID = 2L
  val BITFINEX_NAME = "Bitfinex"
  val BITFINEX_ID = 3L
  val FOREX_NAME = "Forex"
  val FOREX_ID = 4L;
  val marketIdToName = Map(BTCE_ID -> BTCE_NAME, BITSTAMP_ID -> BITSTAMP_NAME, BITFINEX_ID -> BITFINEX_NAME , FOREX_ID->FOREX_NAME)
  val marketNameToId = Map(BTCE_NAME -> BTCE_ID, BITSTAMP_NAME -> BITSTAMP_ID, BITFINEX_NAME -> BITFINEX_ID,FOREX_NAME->FOREX_ID)
}