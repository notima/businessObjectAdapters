package org.notima.businessobjects.adapter.tools.tenant;

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
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Checks the tenants of one or more adapters, one {@link TenantCandidate} per tenant.
 *
 * <p>Handles what is common to such checks: listing the tenants, limiting the check to some
 * tenants ({@link #setOnlyTaxIds(Collection)}), choosing the country code, selecting the
 * tenant in the adapter, reporting progress and catching errors per tenant. Subclasses
 * implement {@link #checkTenant(BusinessObjectFactory, TenantCandidate)}.</p>
 *
 * @param <C>	The type of result per tenant.
 */
public abstract class AbstractTenantCandidateFinder<C extends TenantCandidate> {

	/** Called for each tenant before it's checked, ie to show progress. */
	public interface ProgressListener {
		void checking(String adapterName, BusinessPartner<?> tenant, int index, int count);
	}

	protected final String defaultCountryCode;

	protected String	countryCode;
	/** Only these tenants are checked, by comparable tax id; null to check all. */
	private Map<String,String> onlyTaxIds;

	protected Date	today = new Date();
	private ProgressListener progressListener;

	/**
	 * @param defaultCountryCode	Country code used for tenants without one.
	 */
	protected AbstractTenantCandidateFinder(String defaultCountryCode) {
		this.defaultCountryCode = defaultCountryCode;
	}

	/** @return	A new, empty result for one tenant. */
	protected abstract C newCandidate();

	/**
	 * Checks one tenant. The tenant is already selected in the adapter and the candidate's
	 * adapter name, tax id, name and country code are set.
	 *
	 * @throws Exception	If the tenant can't be checked; the candidate is then marked as an error.
	 */
	protected abstract void checkTenant(BusinessObjectFactory<?,?,?,?,?,?> adapter, C candidate) throws Exception;

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

	/** The date to count days from; today by default. */
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
	 * @return	One candidate per tenant checked, in adapter and tenant order. Requested
	 * 			tenants that no adapter has are added last, as errors.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<C> find(Collection<BusinessObjectFactory> adapters) {

		List<C> result = new ArrayList<C>();
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
				C missing = newCandidate();
				missing.setTaxId(requested.getValue());
				missing.setCountryCode(countryCode != null ? countryCode : defaultCountryCode);
				missing.markError("Not a tenant");
				result.add(missing);
			}
		}
		return result;

	}

	/**
	 * Checks one tenant of an adapter.
	 */
	public C check(BusinessObjectFactory<?,?,?,?,?,?> adapter, BusinessPartner<?> tenant) {

		C candidate = newCandidate();
		candidate.setAdapterName(adapter.getSystemName());
		candidate.setTaxId(tenant.getTaxId());
		candidate.setName(tenant.getName());
		String countryCode = this.countryCode != null ? this.countryCode
				: tenant.getCountryCode() != null && tenant.getCountryCode().trim().length() > 0
				? tenant.getCountryCode().trim() : defaultCountryCode;
		candidate.setCountryCode(countryCode);

		try {
			adapter.setTenant(tenant.getTaxId(), countryCode);
			checkTenant(adapter, candidate);
		} catch (Exception e) {
			candidate.markError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
		}
		return candidate;

	}

	/** Comparable form of a tax id: its letters and digits, upper case. */
	public static String taxIdKey(String taxId) {
		return taxId == null ? "" : taxId.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
	}

	protected static Date startOfDay(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/** {@code days} days after the start of {@code date}'s day. */
	protected static Date addDays(Date date, int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(startOfDay(date));
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}

	protected static long daysBetween(Date from, Date to) {
		return TimeUnit.MILLISECONDS.toDays(to.getTime() - from.getTime());
	}

	/** Calendar days between two dates; rounded, since a day can be 23 or 25 hours at daylight saving time changes. */
	protected static long calendarDaysBetween(Date from, Date to) {
		return Math.round((startOfDay(to).getTime() - startOfDay(from).getTime()) / (double) TimeUnit.DAYS.toMillis(1));
	}

}
