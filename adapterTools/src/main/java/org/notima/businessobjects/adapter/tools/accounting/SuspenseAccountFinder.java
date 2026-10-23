package org.notima.businessobjects.adapter.tools.accounting;

import java.util.EnumSet;
import java.util.Set;

import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.SuspenseAccountType;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.businessobjects.accounting.AccountTotals;
import org.notima.generic.businessobjects.accounting.LedgerCalculator;
import org.notima.generic.businessobjects.accounting.LedgerSummary;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

/**
 * Finds the balances of the suspense accounts of tenants at a date ({@link SuspenseBalances}):
 * the accounts set per type in each tenant's {@link TenantInformation#getSuspenseAccounts()},
 * which should normally be zero. Tenants without suspense accounts (of the types asked for)
 * get status {@link TenantAccountingStatus#NOT_CONFIGURED}, and their accounting isn't read.
 */
public class SuspenseAccountFinder extends AbstractAccountingFinder<SuspenseBalances> {

	private final TenantInformationFactory tenantInformationFactory;
	private Set<SuspenseAccountType> types = EnumSet.allOf(SuspenseAccountType.class);

	/**
	 * @param tenantInformationFactory	Where the tenants' suspense accounts are set.
	 */
	public SuspenseAccountFinder(String defaultCountryCode, AccountingModelService service,
			TenantInformationFactory tenantInformationFactory) {
		super(defaultCountryCode, service);
		this.tenantInformationFactory = tenantInformationFactory;
	}

	/** @param types	Only suspense accounts of these types; null or empty for all. */
	public void setTypes(Set<SuspenseAccountType> types) {
		this.types = types == null || types.isEmpty() ? EnumSet.allOf(SuspenseAccountType.class) : EnumSet.copyOf(types);
	}

	@Override
	protected SuspenseBalances newCandidate() {
		return new SuspenseBalances();
	}

	@Override
	protected void checkTenant(BusinessObjectFactory<?,?,?,?,?,?> adapter, SuspenseBalances candidate) throws Exception {
		TenantInformation ti = tenantInformationFactory != null
				? tenantInformationFactory.getTenantInformation(new TaxSubjectIdentifier(candidate.getTaxId(), candidate.getCountryCode()))
				: null;
		if (ti != null) {
			for (SuspenseAccountType type : types) {
				for (String accountNo : ti.getSuspenseAccounts(type)) {
					candidate.getConfigured().add(new SuspenseBalances.Balance(type, accountNo, null, 0.0, false));
				}
			}
		}
		if (candidate.getConfigured().isEmpty()) {
			candidate.setStatus(TenantAccountingStatus.NOT_CONFIGURED);
			return;
		}
		super.checkTenant(adapter, candidate);
	}

	@Override
	protected void checkPeriod(AccountingPeriodData period, SuspenseBalances c) {
		LedgerSummary s = LedgerCalculator.summarize(period, null, null, date);
		for (SuspenseBalances.Balance configured : c.getConfigured()) {
			AccountTotals t = s.getAccount(configured.getAccountNo());
			c.addBalance(new SuspenseBalances.Balance(configured.getType(), configured.getAccountNo(),
					t != null ? t.getName() : null,
					t != null ? AccountingSummaryFinder.round(t.getBalanceAtTo()) : 0.0,
					t != null));
		}
	}

}
