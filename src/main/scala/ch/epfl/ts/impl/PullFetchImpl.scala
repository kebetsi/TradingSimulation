package ch.epfl.ts.impl

import ch.epfl.ts.first.PullFetch
import ch.epfl.ts.data.Transaction

class PullFetchImpl extends PullFetch[Transaction] {
  override def interval = 12000
	override def fetch: List[Transaction] = {
		return List()
	}
}