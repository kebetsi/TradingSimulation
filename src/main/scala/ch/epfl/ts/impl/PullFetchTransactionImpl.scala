package ch.epfl.ts.impl

import ch.epfl.ts.first.PullFetch
import ch.epfl.ts.data.{Transaction, Currency}

class PullFetchTransactionImpl extends PullFetch[Transaction] {
  override def interval = 12000
	override def fetch: List[Transaction] = {
		return List(Transaction(1.2,1.2, 1, Currency.BTC, "moi", "toi"),
        Transaction(1.3,1.3, 2, Currency.BTC, "moi", "toi"),
        Transaction(1.4,1.4, 3, Currency.BTC, "moi", "toi"))
	}
}
