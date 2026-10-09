package org.notima.businessobjects.adapter.tools.dunning;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceReaderOptions;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Finds the tenants that need a dunning run: tenants with open customer invoices that are
 * overdue.
 *
 * <p>Each tenant's open customer invoices are read with
 * {@link BusinessObjectFactory#readInvoices(OrderInvoiceReaderOptions)} (open, sales only),
 * so it works with any adapter that supports reading open invoices. An invoice is overdue
 * when its open amount is positive and its due date is at least
 * {@link #setMinDaysOverdue(int) minDaysOverdue} days ago (by default: any day past due).</p>
 *
 * <p>The result is a list of {@link DunningCandidate}s, one per tenant checked, for
 * presentation elsewhere.</p>
 */
public class DunningCandidateFinder {

	/** Called for each tenant before it's checked, ie to show progress. */
	public interface ProgressListener {
		void checking(String adapterName, BusinessPartner<?> tenant, int index, int count);
	}

	private final String defaultCountryCode;

	private String	countryCode;
	/** Only these tenants are checked, by comparable tax id; null to check all. */
	private Map<String,String> onlyTaxIds;

	private int		minDaysOverdue = 1;
	private Date	today = new Date();
	private ProgressListener progressListener;

	/**
	 * @param defaultCountryCode	Country code used for tenants without one.
	 */
	public DunningCandidateFinder(String defaultCountryCode) {
		this.defaultCountryCode = defaultCountryCode;
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
	 * @param countryCode	Country code to use for all tenants checked, instead of each
	 * 						tenant's own (or the default). Null to use the tenants' own.
	 */
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode != null && countryCode.trim().length() > 0 ? countryCode.trim() : null;
	}

	/**
	 * Limits the check to these tenants, ie because checking all is slow. Tax ids are
	 * compared by their letters and digits, so "556000-0000" matches "5560000000".
	 * 
	 * @param taxIds	The org numbers / tax ids of the tenants to check. Null or empty to
	 * 					check all tenants.
	 */
	public void setOnlyTaxIds(Collection<String> taxIds) {
		if (taxIds == null || taxIds.isEmpty()) {
			onlyTaxIds = null;
			return;
		}
		onlyTaxIds = new LinkedHashMap<String,String>();
		for (String t : taxIds) {
			if (t != null && t.trim().length() > 0) onlyTaxIds.put(taxIdKey(t), t.trim());
		}
		if (onlyTaxIds.isEmpty()) onlyTaxIds = null;
	}

	/** The date to count overdue days from; today by default. */
	public void setToday(Date today) {
		this.today = today;
	}

	public void setProgressListener(ProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	/**
	 * Checks all tenants of the given adapters.
	 *
	 * @param adapters	The adapters whose tenants to check.
	 * @return	One candidate per tenant checked, in adapter and tenant order.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<DunningCandidate> find(Collection<BusinessObjectFactory> adapters) {

		List<DunningCandidate> result = new ArrayList<DunningCandidate>();
		Set<String> found = new HashSet<String>();
		for (BusinessObjectFactory adapter : adapters) {
			BusinessPartnerList<Object> tenants = adapter.listTenants();
			if (tenants == null || tenants.getBusinessPartner() == null) continue;
			List<BusinessPartner<Object>> list = new ArrayList<BusinessPartner<Object>>();
			for (BusinessPartner<Object> tenant : tenants.getBusinessPartner()) {
				String key = taxIdKey(tenant.getTaxId());
				if (onlyTaxIds == null || onlyTaxIds.containsKey(key)) {
					list.add(tenant);
					found.add(key);
				}
			}
			for (int i = 0; i < list.size(); i++) {
				BusinessPartner<?> tenant = list.get(i);
				if (progressListener != null) {
					progressListener.checking(adapter.getSystemName(), tenant, i + 1, list.size());
				}
				result.add(check(adapter, tenant));
			}
		}
		// Requested tenants that no adapter has, ie a mistyped org number
		if (onlyTaxIds != null) {
			for (Map.Entry<String,String> requested : onlyTaxIds.entrySet()) {
				if (found.contains(requested.getKey())) continue;
				DunningCandidate missing = new DunningCandidate();
				missing.setTaxId(requested.getValue());
				missing.setCountryCode(countryCode != null ? countryCode : defaultCountryCode);
				missing.setStatus(DunningCandidate.Status.ERROR);
				missing.setError("Not a tenant");
				result.add(missing);
			}
		}
		return result;

	}

	/**
	 * Checks one tenant of an adapter.
	 */
	public DunningCandidate check(BusinessObjectFactory<?,?,?,?,?,?> adapter, BusinessPartner<?> tenant) {

		DunningCandidate candidate = new DunningCandidate();
		candidate.setAdapterName(adapter.getSystemName());
		candidate.setTaxId(tenant.getTaxId());
		candidate.setName(tenant.getName());
		String countryCode = this.countryCode != null ? this.countryCode
				: tenant.getCountryCode() != null && tenant.getCountryCode().trim().length() > 0
				? tenant.getCountryCode().trim() : defaultCountryCode;
		candidate.setCountryCode(countryCode);

		try {
			adapter.setTenant(tenant.getTaxId(), countryCode);
			OrderInvoiceReaderOptions opts = new OrderInvoiceReaderOptions();
			opts.setOpenOnly(true);
			opts.setSalesOnly(true);
			opts.setUnpostedOnly(false);
			OrderInvoiceOperationResult read = adapter.readInvoices(opts);
			List<Invoice<?>> open = read != null && read.getAffectedInvoices() != null
					? read.getAffectedInvoices().getInvoiceList() : null;
			summarizeOverdue(candidate, open);
		} catch (Exception e) {
			candidate.setStatus(DunningCandidate.Status.ERROR);
			candidate.setError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
		}
		return candidate;

	}

	/**
	 * Sets the overdue summary and status of {@code candidate} from the tenant's open invoices.
	 */
	void summarizeOverdue(DunningCandidate candidate, List<Invoice<?>> openInvoices) {

		Date dueBefore = dueBefore();
		Set<String> customers = new HashSet<String>();
		Set<String> currencies = new HashSet<String>();
		int count = 0;
		double amount = 0;
		Date oldest = null;

		if (openInvoices != null) {
			for (Invoice<?> inv : openInvoices) {
				if (inv.getOpenAmt() <= 0 || inv.getDueDate() == null || !inv.getDueDate().before(dueBefore)) continue;
				count++;
				amount += inv.getOpenAmt();
				if (inv.getCurrency() != null) currencies.add(inv.getCurrency());
				BusinessPartner<?> bp = inv.getBusinessPartner();
				customers.add(bp != null && bp.getIdentityNo() != null ? bp.getIdentityNo()
						: bp != null && bp.getName() != null ? bp.getName() : "?");
				if (oldest == null || inv.getDueDate().before(oldest)) oldest = inv.getDueDate();
			}
		}

		candidate.setOverdueInvoices(count);
		candidate.setCustomers(customers.size());
		candidate.setOverdueAmount(Math.round(amount * 100) / 100.0);
		candidate.setCurrency(currencies.isEmpty() ? null : currencies.size() == 1 ? currencies.iterator().next() : "*");
		candidate.setOldestDueDate(oldest);
		candidate.setMaxDaysOverdue(oldest != null ? (int) daysBetween(oldest, today) : 0);
		candidate.setStatus(count > 0 ? DunningCandidate.Status.NEEDS_DUNNING : DunningCandidate.Status.NOTHING_OVERDUE);

	}

	/** Comparable form of a tax id: its letters and digits, upper case. */
	static String taxIdKey(String taxId) {
		return taxId == null ? "" : taxId.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
	}

	/** Invoices due before this date are overdue enough to remind. */
	private Date dueBefore() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(today);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, 1 - Math.max(minDaysOverdue, 1));
		return cal.getTime();
	}

	private static long daysBetween(Date from, Date to) {
		return TimeUnit.MILLISECONDS.toDays(to.getTime() - from.getTime());
	}

}
