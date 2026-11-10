package org.notima.businessobjects.adapter.tools.accounting;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.accounting.rule.CountryRuleSet;
import org.notima.generic.businessobjects.tax.vatreport.VatReport;
import org.notima.generic.businessobjects.tax.vatreport.VatReportCalculator;
import org.notima.generic.businessobjects.tax.vatreport.InputVatChecks;
import org.notima.generic.businessobjects.tax.vatreport.TaxKeyOverrides;
import org.notima.generic.businessobjects.tax.vatreport.VatReportDefinition;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;

/**
 * The VAT return of a tenant for a date range, calculated from the accounting model of its
 * accounting system (as cached by refresh-accounting) with the VAT return of its country's
 * rules ({@link CountryRuleSet#getVatReportDefinition()}).
 */
public class TenantVatReporter {

	/** The VAT return of a tenant, or why there is none. */
	public static class Result {
		private TenantAccountingStatus status = TenantAccountingStatus.OK;
		private String statusText;
		private String systemName;
		private final List<String> fiscalYears = new ArrayList<String>();
		private VatReport report;

		public TenantAccountingStatus getStatus() { return status; }
		public boolean isOk() { return status == TenantAccountingStatus.OK; }
		/** Why there is no report; null when there is one. */
		public String getStatusText() { return statusText; }
		/** The accounting system the accounting was read from. */
		public String getSystemName() { return systemName; }
		/** The names of the fiscal years the vouchers were read from. */
		public List<String> getFiscalYears() { return fiscalYears; }
		public VatReport getReport() { return report; }

		private Result fail(TenantAccountingStatus status, String text) {
			this.status = status;
			this.statusText = text;
			return this;
		}
	}

	private final AccountingModelService service;
	private Duration maxAge;

	public TenantVatReporter(AccountingModelService service) {
		this.service = service;
		this.maxAge = service.getMaxAge();
	}

	/**
	 * @param maxAge	Cached models older than this are fetched again; {@link Duration#ZERO}
	 * 					to fetch all. The service's maximum age by default.
	 */
	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge != null ? maxAge : service.getMaxAge();
	}

	/**
	 * @param systemName	Only the accounting of this system (adapter); null for the first
	 * 						provider that has the tenant's accounting.
	 * @param tenant		The tenant, with the country whose VAT return to make.
	 * @param from			First day of the range.
	 * @param to			Last day of the range.
	 */
	public Result report(String systemName, TaxSubjectIdentifier tenant, LocalDate from, LocalDate to) throws Exception {
		return report(systemName, tenant, from, to, null);
	}

	/**
	 * @param vatCodes	The tenant's own VAT codes per account (ie from its
	 * 					{@code TenantInformation}), before those of the chart of accounts; null
	 * 					for none.
	 * @see #report(String, TaxSubjectIdentifier, LocalDate, LocalDate)
	 */
	public Result report(String systemName, TaxSubjectIdentifier tenant, LocalDate from, LocalDate to,
			Map<String, String> vatCodes) throws Exception {
		return report(systemName, tenant, from, to, vatCodes, null);
	}

	/**
	 * @param inputVatChecks	The accounts whose input VAT must be deducted and the rate in
	 * 							percent (mixed activity), ie from its {@code TenantInformation}; null
	 * 							for none.
	 * @see #report(String, TaxSubjectIdentifier, LocalDate, LocalDate, Map)
	 */
	public Result report(String systemName, TaxSubjectIdentifier tenant, LocalDate from, LocalDate to,
			Map<String, String> vatCodes, Map<String, BigDecimal> inputVatChecks) throws Exception {
		Result result = new Result();

		CountryRuleSet rules = service.getRuleSets().lookup(tenant.getCountryCode());
		VatReportDefinition definition = rules != null ? rules.getVatReportDefinition() : null;
		if (definition != null) definition = InputVatChecks.of(TaxKeyOverrides.of(definition, vatCodes), inputVatChecks);
		if (definition == null) {
			return result.fail(TenantAccountingStatus.NOT_CONFIGURED, "No VAT report for country " + tenant.getCountryCode());
		}

		AccountingModelProvider provider = service.providerFor(systemName, tenant);
		if (provider == null) {
			return result.fail(TenantAccountingStatus.NO_ACCOUNTING, "No accounting found for " + tenant.getTaxId()
					+ (systemName != null ? " in " + systemName : ""));
		}
		result.systemName = provider.getSystemName();

		List<AccountingPeriodData> periods = service.periodsBetween(provider, tenant, from, to, maxAge);
		if (periods.isEmpty()) {
			return result.fail(TenantAccountingStatus.NO_FISCAL_YEAR, "No fiscal year between " + from + " and " + to);
		}
		for (AccountingPeriodData p : periods) {
			result.fiscalYears.add(p.getName() != null ? p.getName() : p.getPeriodStart() + " - " + p.getPeriodEnd());
		}
		result.report = VatReportCalculator.calculate(definition, VatReportCalculator.periodsFor(periods, null, from, to), from, to);
		return result;
	}

}
