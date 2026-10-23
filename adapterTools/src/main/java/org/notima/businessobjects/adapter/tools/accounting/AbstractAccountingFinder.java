package org.notima.businessobjects.adapter.tools.accounting;

import java.time.Duration;
import java.time.LocalDate;

import org.notima.businessobjects.adapter.tools.tenant.AbstractTenantCandidateFinder;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Checks the accounting of tenants: for each tenant, reads the period of the fiscal year that
 * contains {@link #setDate(LocalDate) the date} from the provider of its adapter's system,
 * through the {@link AccountingModelService}, and hands it to {@link #checkPeriod}.
 *
 * @param <C>	The result per tenant.
 */
public abstract class AbstractAccountingFinder<C extends AbstractAccountingCandidate> extends AbstractTenantCandidateFinder<C> {

	protected final AccountingModelService service;
	protected LocalDate date = LocalDate.now();
	private Duration maxAge;

	protected AbstractAccountingFinder(String defaultCountryCode, AccountingModelService service) {
		super(defaultCountryCode);
		this.service = service;
		this.maxAge = service.getMaxAge();
	}

	/** @param date	The date to read the accounting at; today by default. */
	public void setDate(LocalDate date) {
		this.date = date != null ? date : LocalDate.now();
	}

	public LocalDate getDate() {
		return date;
	}

	/**
	 * @param maxAge	Cached models older than this are fetched again; {@link Duration#ZERO}
	 * 					to fetch all. The service's maximum age by default.
	 */
	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge != null ? maxAge : service.getMaxAge();
	}

	public Duration getMaxAge() {
		return maxAge;
	}

	@Override
	protected void checkTenant(BusinessObjectFactory<?,?,?,?,?,?> adapter, C candidate) throws Exception {
		TaxSubjectIdentifier tenant = new TaxSubjectIdentifier(candidate.getTaxId(), candidate.getCountryCode());
		AccountingModelProvider provider = service.providerFor(adapter.getSystemName(), tenant);
		if (provider == null) {
			candidate.setStatus(TenantAccountingStatus.NO_ACCOUNTING);
			return;
		}
		checkProvider(provider, tenant, candidate);
	}

	/**
	 * Checks a tenant whose accounting {@code provider} has. By default reads the period of
	 * {@link #getDate()} and calls {@link #checkPeriod}.
	 */
	protected void checkProvider(AccountingModelProvider provider, TaxSubjectIdentifier tenant, C candidate) throws Exception {
		AccountingPeriod fy = service.fiscalYearOf(provider, tenant, date, maxAge);
		if (fy == null) {
			candidate.setStatus(TenantAccountingStatus.NO_FISCAL_YEAR);
			return;
		}
		AccountingPeriodData period = AccountingModelService.periodOf(service.model(provider, tenant, fy, maxAge), fy);
		if (period == null) {
			candidate.setStatus(TenantAccountingStatus.NO_FISCAL_YEAR);
			return;
		}
		candidate.setFiscalYear(fy.getName());
		checkPeriod(period, candidate);
		candidate.setStatus(TenantAccountingStatus.OK);
	}

	/** Fills in {@code candidate} from the period of the tenant's fiscal year at {@link #getDate()}. */
	protected abstract void checkPeriod(AccountingPeriodData period, C candidate);

}
