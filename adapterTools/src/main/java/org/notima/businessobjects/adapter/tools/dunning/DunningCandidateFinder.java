package org.notima.businessobjects.adapter.tools.dunning;

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
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

/**
 * Finds the tenants that need a dunning run: tenants with open customer invoices that are
 * overdue.
 *
 * <p>Each tenant's open customer invoices are read with
 * {@link BusinessObjectFactory#readInvoices(OrderInvoiceReaderOptions)} (open, sales only),
 * so it works with any adapter that supports reading open invoices. An invoice is overdue
 * when its open amount is positive and its due date is at least
 * {@link #setMinDaysOverdue(int) minDaysOverdue} days ago (by default: any day past due).
 * A customer's overpaid invoices and credit notes are deducted, so that only customers
 * that owe something in total are counted.</p>
 *
 * <p>The result is a list of {@link DunningCandidate}s, one per tenant checked, for
 * presentation elsewhere.</p>
 */
public class DunningCandidateFinder extends AbstractTenantCandidateFinder<DunningCandidate> {

	/** By default, reminders aren't sent more often than this. */
	public static final int DEFAULT_DUNNING_INTERVAL_DAYS = 7;

	private int		minDaysOverdue = 1;
	private int		dunningIntervalDays = DEFAULT_DUNNING_INTERVAL_DAYS;
	private TenantInformationFactory tenantInformationFactory;

	/**
	 * @param defaultCountryCode	Country code used for tenants without one.
	 */
	public DunningCandidateFinder(String defaultCountryCode) {
		super(defaultCountryCode);
	}

	/**
	 * @param days	How many days past the due date an invoice must be to be reminded.
	 * 				1 (the default) means any invoice whose due date has passed.
	 */
	public void setMinDaysOverdue(int days) {
		this.minDaysOverdue = days;
	}

	public int getMinDaysOverdue() {
		return minDaysOverdue;
	}

	/**
	 * @param days	Tenants reminded fewer days ago than this get status
	 * 				{@link DunningCandidate.Status#RECENTLY_DUNNED} instead of
	 * 				{@link DunningCandidate.Status#NEEDS_DUNNING}. 0 to ignore when tenants were reminded.
	 */
	public void setDunningIntervalDays(int days) {
		this.dunningIntervalDays = days;
	}

	public int getDunningIntervalDays() {
		return dunningIntervalDays;
	}

	/**
	 * @param tif	Where the tenants' last dunning dates are read from. Null to ignore when
	 * 				tenants were reminded.
	 */
	public void setTenantInformationFactory(TenantInformationFactory tif) {
		this.tenantInformationFactory = tif;
	}

	@Override
	protected DunningCandidate newCandidate() {
		return new DunningCandidate();
	}

	@Override
	protected void checkTenant(BusinessObjectFactory<?,?,?,?,?,?> adapter, DunningCandidate candidate) throws Exception {

		OrderInvoiceReaderOptions opts = new OrderInvoiceReaderOptions();
		opts.setOpenOnly(true);
		opts.setSalesOnly(true);
		opts.setUnpostedOnly(false);
		OrderInvoiceOperationResult read = adapter.readInvoices(opts);
		List<Invoice<?>> open = read != null && read.getAffectedInvoices() != null
				? read.getAffectedInvoices().getInvoiceList() : null;
		summarizeOverdue(candidate, open);
		applyLastDunningDate(candidate);

	}

	/**
	 * Sets the overdue summary and status of {@code candidate} from the tenant's open invoices.
	 * A customer is only counted when it owes something in total: its overpaid invoices and
	 * credit notes (negative open amounts), due or not, are deducted from its overdue amount.
	 * A customer whose credits cover the overdue invoices isn't reminded by create-dunning-run
	 * and isn't counted here.
	 */
	void summarizeOverdue(DunningCandidate candidate, List<Invoice<?>> openInvoices) {

		Date dueBefore = dueBefore();
		Map<String,CustomerBalance> balances = new LinkedHashMap<String,CustomerBalance>();

		if (openInvoices != null) {
			for (Invoice<?> inv : openInvoices) {
				double open = inv.getOpenAmt();
				if (open < 0) {
					balanceFor(balances, inv).credits += open;
				} else if (open > 0 && inv.getDueDate() != null && inv.getDueDate().before(dueBefore)) {
					balanceFor(balances, inv).addOverdue(inv);
				}
			}
		}

		Set<String> currencies = new HashSet<String>();
		int count = 0;
		int customers = 0;
		double amount = 0;
		Date oldest = null;

		for (CustomerBalance b : balances.values()) {
			double owed = b.overdue + b.credits;
			if (b.invoices == 0 || owed < 0.005) continue;
			customers++;
			count += b.invoices;
			amount += owed;
			currencies.addAll(b.currencies);
			if (oldest == null || b.oldest.before(oldest)) oldest = b.oldest;
		}

		candidate.setOverdueInvoices(count);
		candidate.setCustomers(customers);
		candidate.setOverdueAmount(Math.round(amount * 100) / 100.0);
		candidate.setCurrency(currencies.isEmpty() ? null : currencies.size() == 1 ? currencies.iterator().next() : "*");
		candidate.setOldestDueDate(oldest);
		candidate.setMaxDaysOverdue(oldest != null ? (int) daysBetween(oldest, today) : 0);
		candidate.setStatus(count > 0 ? DunningCandidate.Status.NEEDS_DUNNING : DunningCandidate.Status.NOTHING_OVERDUE);

	}

	/**
	 * Sets the tenant's last dunning date on {@code candidate}, and status
	 * {@link DunningCandidate.Status#RECENTLY_DUNNED} if it needs dunning but was reminded
	 * within the dunning interval.
	 */
	void applyLastDunningDate(DunningCandidate candidate) {

		if (tenantInformationFactory == null) return;
		TenantInformation ti = tenantInformationFactory.getTenantInformation(
				new TaxSubjectIdentifier(candidate.getTaxId(), candidate.getCountryCode()));
		if (ti == null || ti.getLastDunningDate() == null) return;
		candidate.setLastDunningDate(ti.getLastDunningDate());
		if (candidate.needsDunning()
				&& calendarDaysBetween(ti.getLastDunningDate(), today) < dunningIntervalDays) {
			candidate.setStatus(DunningCandidate.Status.RECENTLY_DUNNED);
		}

	}

	/** A customer's overdue invoices and credits. */
	private static class CustomerBalance {
		double overdue;
		double credits;
		int invoices;
		Date oldest;
		Set<String> currencies = new HashSet<String>();

		void addOverdue(Invoice<?> inv) {
			overdue += inv.getOpenAmt();
			invoices++;
			if (inv.getCurrency() != null) currencies.add(inv.getCurrency());
			if (oldest == null || inv.getDueDate().before(oldest)) oldest = inv.getDueDate();
		}
	}

	private static CustomerBalance balanceFor(Map<String,CustomerBalance> balances, Invoice<?> inv) {
		BusinessPartner<?> bp = inv.getBusinessPartner();
		String key = bp != null && bp.getIdentityNo() != null ? bp.getIdentityNo()
				: bp != null && bp.getName() != null ? bp.getName() : "?";
		CustomerBalance b = balances.get(key);
		if (b == null) {
			b = new CustomerBalance();
			balances.put(key, b);
		}
		return b;
	}

	/** Invoices due before this date are overdue enough to remind. */
	private Date dueBefore() {
		return addDays(today, 1 - Math.max(minDaysOverdue, 1));
	}

}
