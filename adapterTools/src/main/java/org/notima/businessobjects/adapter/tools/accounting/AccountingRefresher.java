package org.notima.businessobjects.adapter.tools.accounting;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.AccountingFileData;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;

/**
 * Fetches the accounting of tenants from their accounting systems into the cache of the
 * {@link AccountingModelService}: the fiscal year that contains the date, or all fiscal years.
 * Fiscal years cached more recently than the maximum age ({@link Duration#ZERO} by default,
 * ie all) are left as they are.
 */
public class AccountingRefresher extends AbstractAccountingFinder<AccountingRefreshResult> {

	private boolean allYears;

	public AccountingRefresher(String defaultCountryCode, AccountingModelService service) {
		super(defaultCountryCode, service);
		setMaxAge(Duration.ZERO);
	}

	/** @param allYears	Refresh all fiscal years of the tenants, not only the one of the date. */
	public void setAllYears(boolean allYears) {
		this.allYears = allYears;
	}

	@Override
	protected AccountingRefreshResult newCandidate() {
		return new AccountingRefreshResult();
	}

	@Override
	protected void checkProvider(AccountingModelProvider provider, TaxSubjectIdentifier tenant, AccountingRefreshResult c) throws Exception {

		// The list of fiscal years is always fetched, a new year may have been added
		List<AccountingPeriod> years = new ArrayList<AccountingPeriod>();
		for (AccountingPeriod fy : service.fiscalYears(provider, tenant, Duration.ZERO)) {
			if (allYears || AccountingModelService.contains(fy, date)) years.add(fy);
		}
		if (years.isEmpty()) {
			c.setStatus(TenantAccountingStatus.NO_FISCAL_YEAR);
			return;
		}
		for (AccountingPeriod fy : years) {
			Duration age = service.getCache().modelAge(provider.getSystemName(), tenant, fy.getPeriodStart());
			if (age != null && !getMaxAge().isZero() && age.compareTo(getMaxAge()) <= 0) {
				c.setUpToDate(c.getUpToDate() + 1);
				continue;
			}
			AccountingFileData data = service.refresh(provider, tenant, fy);
			AccountingPeriodData period = AccountingModelService.periodOf(data, fy);
			c.setRefreshed(c.getRefreshed() + 1);
			if (period != null) c.setVouchers(c.getVouchers() + period.getVouchers().size());
		}
		c.setFiscalYear(years.size() == 1 ? years.get(0).getName()
				: years.get(0).getName() + " – " + years.get(years.size() - 1).getName());
		c.setStatus(TenantAccountingStatus.OK);

	}

	@Override
	protected void checkPeriod(AccountingPeriodData period, AccountingRefreshResult candidate) {
		// Not used, checkProvider refreshes
	}

}
