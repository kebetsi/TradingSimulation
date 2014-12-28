package ch.epfl.ts.whatisthis

import ch.epfl.ts.component.fetch.PullFetch
import ch.epfl.ts.data.{Transaction, Currency}

class PullFetchTransactionImpl extends PullFetch[Transaction] {
  override def interval = 12000
	override def fetch: List[Transaction] = {
		List(Transaction(1.2,1.2, 1, Currency.BTC, 1,1,2,2),
        Transaction(1.3,1.3, 2, Currency.BTC, 1,3,2,4),
        Transaction(1.4,1.4, 3, Currency.BTC, 1,5,2,6))
	}
}
