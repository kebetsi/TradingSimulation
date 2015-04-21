package ch.epfl.ts.data

/**
 * Enum for Currencies
 */
object Currency extends Enumeration {
  type Currency = Value
  // Cryptocurrencies
  val BTC = Value("btc")
  val LTC = Value("ltc")
  
  // Real-life currencies
  val USD = Value("usd")
  val CHF = Value("chf")
  val RUR = Value("rur")
  val EUR = Value("eur")
  val JPY = Value("jpy")
  val GBP = Value("gbp")
  val AUD = Value("aud")
  val CAD = Value("cad")
  
  // Fallback ("default")
  val DEF = Value("def")
  
  def fromString(s: String): Currency = {
    this.values.find(v => v.toString().toLowerCase() == s.toLowerCase()) match {
      case Some(currency) => currency
      case None => {
        throw new UnsupportedOperationException("Currency " + s + " is not supported.")
      }
    }
  }
  
  /**
   * Creates a tuple of currencies given a string
   *
   * @param   s   Input string of length six, three characters for each currency. Case insensitive.
   *              Example: "EURCHF" returns (Currency.EUR, Currency.CHF)
   */
  def pairFromString(s: String): (Currency, Currency) = {
    ( Currency.fromString(s.slice(0, 3)), Currency.fromString(s.slice(3,6)) )
  }
}
