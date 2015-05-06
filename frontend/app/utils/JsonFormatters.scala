package utils

import ch.epfl.ts.data.OHLC
import play.api.libs.json._

object JsonFormatters {
  implicit val ohlcFormat: Format[OHLC] = Json.format[OHLC]
}