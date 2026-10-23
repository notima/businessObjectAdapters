package org.notima.businessobjects.adapter.tools.accounting;

import java.time.LocalDate;

/**
 * Key figures of the accounting of one tenant at a date, from the start of its fiscal year.
 * Produced by {@link AccountingSummaryFinder}. Amounts have their natural signs: revenue,
 * profit, equity and cash are positive.
 */
public class AccountingSummary extends AbstractAccountingCandidate {

	private double		revenue;
	private double		result;
	private double		equity;
	private double		cash;
	private LocalDate	lastVoucherDate;
	private int			vouchers;

	/** @return	The revenue of the fiscal year until the date. */
	public double getRevenue() {
		return revenue;
	}
	public void setRevenue(double revenue) {
		this.revenue = revenue;
	}
	/** @return	The result (profit positive) of the fiscal year until the date. */
	public double getResult() {
		return result;
	}
	public void setResult(double result) {
		this.result = result;
	}
	/** @return	The booked equity at the date, not including the result of the year until it's booked. */
	public double getEquity() {
		return equity;
	}
	public void setEquity(double equity) {
		this.equity = equity;
	}
	/** @return	The balance of the cash and bank accounts at the date. */
	public double getCash() {
		return cash;
	}
	public void setCash(double cash) {
		this.cash = cash;
	}
	/** @return	The date of the last voucher on or before the date; null if none. */
	public LocalDate getLastVoucherDate() {
		return lastVoucherDate;
	}
	public void setLastVoucherDate(LocalDate lastVoucherDate) {
		this.lastVoucherDate = lastVoucherDate;
	}
	/** @return	The number of vouchers of the fiscal year until the date. */
	public int getVouchers() {
		return vouchers;
	}
	public void setVouchers(int vouchers) {
		this.vouchers = vouchers;
	}

}
