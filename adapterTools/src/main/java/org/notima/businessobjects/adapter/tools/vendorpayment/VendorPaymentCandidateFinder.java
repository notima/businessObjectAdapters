package org.notima.businessobjects.adapter.tools.vendorpayment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.notima.businessobjects.adapter.tools.tenant.AbstractTenantCandidateFinder;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceReaderOptions;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Finds the tenants that have vendor invoices to pay: open vendor invoices that are overdue
 * or fall due within the period checked.
 *
 * <p>Each tenant's open vendor invoices are read with
 * {@link BusinessObjectFactory#readVendorInvoices(OrderInvoiceReaderOptions)}, so it works
 * with any adapter that supports reading open vendor invoices. The period ends
 * {@link #setDaysAhead(int) daysAhead} days from today (by default
 * {@value #DEFAULT_DAYS_AHEAD}), or at the {@link #setPaymentDate(Date) payment date} if set.
 * A supplier's credit notes are deducted, so that only suppliers that are owed something in
 * total are counted.</p>
 *
 * <p>Unbooked invoices are read separately (unposted only), since they may need booking or
 * approval before they can be paid. They're counted, and shown as unbooked, unless
 * {@link #setIncludeUnbooked(boolean) excluded}.</p>
 */
public class VendorPaymentCandidateFinder extends AbstractTenantCandidateFinder<VendorPaymentCandidate> {

	/** By default, invoices that fall due within this many days are counted. */
	public static final int DEFAULT_DAYS_AHEAD = 7;

	private int		daysAhead = DEFAULT_DAYS_AHEAD;
	private Date	paymentDate;
	private boolean	includeOverdue = true;
	private boolean	includeUnbooked = true;

	/**
	 * @param defaultCountryCode	Country code used for tenants without one.
	 */
	public VendorPaymentCandidateFinder(String defaultCountryCode) {
		super(defaultCountryCode);
	}

	/**
	 * @param days	Invoices that fall due within this many days from today are counted.
	 * 				0 means only invoices due today (and overdue ones).
	 */
	public void setDaysAhead(int days) {
		this.daysAhead = Math.max(days, 0);
	}

	public int getDaysAhead() {
		return daysAhead;
	}

	/**
	 * @param paymentDate	Count the invoices that fall due on or before this date, instead
	 * 						of using {@link #setDaysAhead(int) daysAhead}. Null to use daysAhead.
	 */
	public void setPaymentDate(Date paymentDate) {
		this.paymentDate = paymentDate;
	}

	/**
	 * @return	The last due date counted.
	 */
	public Date getLastDueDate() {
		return paymentDate != null ? startOfDay(paymentDate) : addDays(today, daysAhead);
	}

	/**
	 * @param includeOverdue	If false, only invoices that fall due within the period, not
	 * 							before it, are counted. True by default.
	 */
	public void setIncludeOverdue(boolean includeOverdue) {
		this.includeOverdue = includeOverdue;
	}

	/**
	 * @param includeUnbooked	If false, unbooked invoices aren't counted. True by default.
	 */
	public void setIncludeUnbooked(boolean includeUnbooked) {
		this.includeUnbooked = includeUnbooked;
	}

	@Override
	protected VendorPaymentCandidate newCandidate() {
		return new VendorPaymentCandidate();
	}

	@Override
	protected void checkTenant(BusinessObjectFactory<?,?,?,?,?,?> adapter, VendorPaymentCandidate candidate) throws Exception {

		OrderInvoiceReaderOptions opts = new OrderInvoiceReaderOptions();
		opts.setVendorOnly(true);
		opts.setOpenOnly(true);
		opts.setUnpostedOnly(false);
		List<Invoice<?>> open = invoices(adapter.readVendorInvoices(opts));

		opts = new OrderInvoiceReaderOptions();
		opts.setVendorOnly(true);
		opts.setUnpostedOnly(true);
		List<Invoice<?>> unbooked = invoices(adapter.readVendorInvoices(opts));

		// Unbooked invoices may or may not be among the open ones, depending on the adapter
		Map<Object,Invoice<?>> all = new LinkedHashMap<Object,Invoice<?>>();
		Set<Object> unbookedKeys = new HashSet<Object>();
		for (Invoice<?> inv : open) {
			all.put(key(inv), inv);
		}
		for (Invoice<?> inv : unbooked) {
			Object key = key(inv);
			unbookedKeys.add(key);
			if (!all.containsKey(key)) all.put(key, inv);
		}
		summarize(candidate, all.values(), unbookedKeys);

	}

	private static List<Invoice<?>> invoices(OrderInvoiceOperationResult read) {
		return read != null && read.getAffectedInvoices() != null && read.getAffectedInvoices().getInvoiceList() != null
				? read.getAffectedInvoices().getInvoiceList() : new ArrayList<Invoice<?>>();
	}

	/** Identifies an invoice in both reads; the invoice itself if it has no document key. */
	static Object key(Invoice<?> inv) {
		return inv.getDocumentKey() != null ? inv.getDocumentKey() : inv;
	}

	/**
	 * Sets the summary and status of {@code candidate} from the tenant's open vendor invoices.
	 * A supplier is only counted when it's owed something in total: its credit notes
	 * (negative open amounts), due or not, are deducted from what is due to it.
	 *
	 * @param openInvoices	The tenant's open vendor invoices, booked and unbooked.
	 * @param unbookedKeys	The {@link #key(Invoice) keys} of the unbooked ones.
	 */
	void summarize(VendorPaymentCandidate candidate, Collection<Invoice<?>> openInvoices, Set<Object> unbookedKeys) {

		Date startOfToday = startOfDay(today);
		Date dueBefore = addDays(getLastDueDate(), 1);
		Map<String,SupplierBalance> balances = new LinkedHashMap<String,SupplierBalance>();

		if (openInvoices != null) {
			for (Invoice<?> inv : openInvoices) {
				boolean isUnbooked = unbookedKeys != null && unbookedKeys.contains(key(inv));
				if (isUnbooked && !includeUnbooked) continue;
				double open = inv.getOpenAmt();
				if (open < 0) {
					balanceFor(balances, inv).credits += open;
					continue;
				}
				if (open <= 0) continue;
				// Without a due date, the invoice is considered due now
				Date due = inv.getDueDate();
				boolean overdue = due != null && due.before(startOfToday);
				if (overdue && !includeOverdue) continue;
				if (due == null || due.before(dueBefore)) {
					balanceFor(balances, inv).addDue(inv, overdue, isUnbooked);
				}
			}
		}

		Set<String> currencies = new HashSet<String>();
		int invoices = 0, suppliers = 0, overdueInvoices = 0, unbookedInvoices = 0;
		double amount = 0, overdueAmount = 0;
		Date earliest = null;

		for (SupplierBalance b : balances.values()) {
			double owed = b.due + b.credits;
			if (b.invoices == 0 || owed < 0.005) continue;
			suppliers++;
			invoices += b.invoices;
			overdueInvoices += b.overdueInvoices;
			unbookedInvoices += b.unbookedInvoices;
			amount += owed;
			overdueAmount += Math.min(b.overdue, owed);
			currencies.addAll(b.currencies);
			if (b.earliest != null && (earliest == null || b.earliest.before(earliest))) earliest = b.earliest;
		}

		candidate.setInvoices(invoices);
		candidate.setSuppliers(suppliers);
		candidate.setAmount(round(amount));
		candidate.setOverdueInvoices(overdueInvoices);
		candidate.setOverdueAmount(round(overdueAmount));
		candidate.setUnbookedInvoices(unbookedInvoices);
		candidate.setCurrency(currencies.isEmpty() ? null : currencies.size() == 1 ? currencies.iterator().next() : "*");
		candidate.setEarliestDueDate(earliest);
		candidate.setMaxDaysOverdue(earliest != null && earliest.before(startOfToday) ? (int) calendarDaysBetween(earliest, today) : 0);
		candidate.setStatus(overdueInvoices > 0 ? VendorPaymentCandidate.Status.OVERDUE
				: invoices > 0 ? VendorPaymentCandidate.Status.DUE_SOON
				: VendorPaymentCandidate.Status.NOTHING_DUE);

	}

	/** A supplier's invoices to pay and credits. */
	private static class SupplierBalance {
		double due;
		double overdue;
		double credits;
		int invoices;
		int overdueInvoices;
		int unbookedInvoices;
		Date earliest;
		Set<String> currencies = new HashSet<String>();

		void addDue(Invoice<?> inv, boolean isOverdue, boolean isUnbooked) {
			due += inv.getOpenAmt();
			invoices++;
			if (isOverdue) {
				overdue += inv.getOpenAmt();
				overdueInvoices++;
			}
			if (isUnbooked) unbookedInvoices++;
			if (inv.getCurrency() != null) currencies.add(inv.getCurrency());
			if (inv.getDueDate() != null && (earliest == null || inv.getDueDate().before(earliest))) earliest = inv.getDueDate();
		}
	}

	private static SupplierBalance balanceFor(Map<String,SupplierBalance> balances, Invoice<?> inv) {
		BusinessPartner<?> bp = inv.getBusinessPartner();
		String key = bp != null && bp.getIdentityNo() != null ? bp.getIdentityNo()
				: bp != null && bp.getName() != null ? bp.getName() : "?";
		SupplierBalance b = balances.get(key);
		if (b == null) {
			b = new SupplierBalance();
			balances.put(key, b);
		}
		return b;
	}

	private static double round(double amount) {
		return Math.round(amount * 100) / 100.0;
	}

}
