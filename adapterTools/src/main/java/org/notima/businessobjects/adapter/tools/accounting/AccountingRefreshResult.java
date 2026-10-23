package org.notima.businessobjects.adapter.tools.accounting;

/** The result of refreshing the cached accounting of one tenant. Produced by {@link AccountingRefresher}. */
public class AccountingRefreshResult extends AbstractAccountingCandidate {

	private int refreshed;
	private int upToDate;
	private int vouchers;

	/** @return	The number of fiscal years fetched from the accounting system. */
	public int getRefreshed() {
		return refreshed;
	}
	public void setRefreshed(int refreshed) {
		this.refreshed = refreshed;
	}
	/** @return	The number of fiscal years not fetched, since their cache was young enough. */
	public int getUpToDate() {
		return upToDate;
	}
	public void setUpToDate(int upToDate) {
		this.upToDate = upToDate;
	}
	/** @return	The number of vouchers in the fiscal years fetched. */
	public int getVouchers() {
		return vouchers;
	}
	public void setVouchers(int vouchers) {
		this.vouchers = vouchers;
	}

}
