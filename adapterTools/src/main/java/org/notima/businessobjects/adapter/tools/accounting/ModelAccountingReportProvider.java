package org.notima.businessobjects.adapter.tools.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.AccountElement.AccountElementType;
import org.notima.generic.businessobjects.AccountStatementLine;
import org.notima.generic.businessobjects.AccountStatementLines;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.BalanceSheetLine;
import org.notima.generic.businessobjects.BalanceSheetReport;
import org.notima.generic.businessobjects.BasicAccountingReportProvider;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.ProfitLossLine;
import org.notima.generic.businessobjects.ProfitLossReport;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.accounting.AccountLedger;
import org.notima.generic.businessobjects.accounting.AccountTotals;
import org.notima.generic.businessobjects.accounting.LedgerCalculator;
import org.notima.generic.businessobjects.accounting.LedgerSummary;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;

/**
 * Accounting reports for any tenant whose accounting an {@link AccountingModelProvider}
 * provides, calculated from its accounting model with {@link LedgerCalculator}. Makes the
 * report commands (show-balance-report, show-pl-report, show-gl-details,
 * list-accounting-vouchers, show-accounting-voucher) work for those tenants.
 *
 * <p>A report covers one fiscal year: the one that contains the end of the requested period
 * (else its start, else today). The period is limited to that fiscal year; a missing start
 * or end is the start or end of the fiscal year.</p>
 */
public class ModelAccountingReportProvider extends BasicAccountingReportProvider {

	private final AccountingModelService service;

	public ModelAccountingReportProvider(AccountingModelService service) {
		this.service = service;
	}

	@Override
	public boolean hasAccountingFor(BusinessPartner<?> bp) {
		return bp != null && service.providerFor(null, new TaxSubjectIdentifier(bp)) != null;
	}

	@Override
	public BalanceSheetReport getBalanceSheet(BusinessPartner<?> bp, AccountingPeriod period,
			AccountingPeriod comparisonPeriod, Properties props) throws Exception {

		Scope scope = scope(bp, period);
		LedgerSummary s = LedgerCalculator.summarize(scope.data, AccountElementType.BalanceAcct, scope.from, scope.to);
		List<BalanceSheetLine> lines = new ArrayList<BalanceSheetLine>();
		for (AccountTotals t : s.getAccounts()) {
			if (t.getBalanceAtTo() == 0.0) continue;
			BalanceSheetLine line = new BalanceSheetLine();
			line.setAccountNo(t.getAccountNo());
			line.setDescription(t.getName());
			line.setAccountClass(t.getCategory().name());
			line.addAmountForPeriod(amount(t.getBalanceAtTo()), scope.period);
			lines.add(line);
		}
		BalanceSheetReport report = new BalanceSheetReport();
		report.setReporter(bp);
		report.setReportPeriod(scope.period);
		report.setLines(lines);
		return report;

	}

	@Override
	public ProfitLossReport getProfitLossReport(BusinessPartner<?> bp, AccountingPeriod period,
			AccountingPeriod comparisonPeriod, Properties props) throws Exception {

		Scope scope = scope(bp, period);
		LedgerSummary s = LedgerCalculator.summarize(scope.data, AccountElementType.PLAcct, scope.from, scope.to);
		List<ProfitLossLine> lines = new ArrayList<ProfitLossLine>();
		for (AccountTotals t : s.getAccounts()) {
			if (t.hasNoMovements()) continue;
			ProfitLossLine line = new ProfitLossLine();
			line.setAccountNo(t.getAccountNo());
			line.setDescription(t.getName());
			line.setAccountClass(t.getCategory().name());
			line.addAmountForPeriod(amount(t.getNet()), scope.period);
			lines.add(line);
		}
		ProfitLossReport report = new ProfitLossReport();
		report.setReporter(bp);
		report.setReportPeriod(scope.period);
		report.setLines(lines);
		return report;

	}

	@Override
	public AccountingVoucher getAccountingVoucher(BusinessPartner<?> bp, AccountingPeriod period, String series,
			String voucherNo) throws Exception {

		Scope scope = scope(bp, period);
		for (AccountingVoucher v : scope.data.getVouchers()) {
			if (voucherNo != null && voucherNo.equals(v.getVoucherNo())
					&& (series == null || series.equalsIgnoreCase(v.getVoucherSeries()))) {
				return v;
			}
		}
		return null;

	}

	@Override
	public List<AccountingVoucher> getAccountingVoucherList(BusinessPartner<?> bp, AccountingPeriod period, String series)
			throws Exception {

		Scope scope = scope(bp, period);
		List<AccountingVoucher> result = new ArrayList<AccountingVoucher>();
		for (AccountingVoucher v : scope.data.getVouchers()) {
			if (series != null && !series.equalsIgnoreCase(v.getVoucherSeries())) continue;
			LocalDate d = v.getAcctDate();
			if (d != null && (d.isBefore(scope.from) || d.isAfter(scope.to))) continue;
			result.add(v);
		}
		return result;

	}

	@Override
	public AccountStatementLines getAccountStatementLines(BusinessPartner<?> bp, String accountNo, LocalDate fromDate,
			LocalDate untilDate, boolean orderByAmount) throws Exception {

		AccountingPeriod requested = new AccountingPeriod();
		requested.setPeriodStart(fromDate);
		requested.setPeriodEnd(untilDate);
		Scope scope = scope(bp, requested);

		double periodOpening = 0.0;
		String name = null;
		for (AccountElement ae : scope.data.getChartOfAccounts()) {
			if (accountNo.equals(ae.getAccountNo())) {
				periodOpening = ae.getOpeningBalance();
				name = ae.getName();
				break;
			}
		}
		AccountLedger ledger = LedgerCalculator.accountLedger(scope.data.getVouchers(), accountNo, name,
				periodOpening, scope.from, scope.to);

		List<AccountStatementLine> lines = new ArrayList<AccountStatementLine>();
		for (AccountLedger.Entry e : ledger.getEntries()) {
			AccountStatementLine line = new AccountStatementLine();
			line.setAccountDate(e.getDate());
			line.setThisAccountNo(accountNo);
			line.setOtherAccountNo(otherAccount(e.getVoucher(), accountNo));
			line.setVoucherSeries(e.getVoucher().getVoucherSeries());
			line.setVoucherNo(e.getVoucher().getVoucherNo());
			line.setDescription(e.getLine().getDescription() != null && !e.getLine().getDescription().isEmpty()
					? e.getLine().getDescription() : e.getVoucher().getDescription());
			line.setTrxAmount(e.getDebit() - e.getCredit());
			lines.add(line);
		}
		if (orderByAmount) {
			lines.sort(Comparator.comparing(AccountStatementLine::getAccountDate, Comparator.nullsLast(Comparator.naturalOrder()))
					.thenComparingDouble(AccountStatementLine::getTrxAmount));
		}
		double balance = ledger.getOpeningBalance();
		for (int i = 0; i < lines.size(); i++) {
			balance += lines.get(i).getTrxAmount();
			lines.get(i).setBalanceAmount(balance);
			lines.get(i).setLineNumber(i + 1);
		}

		AccountStatementLines result = new AccountStatementLines();
		result.setStartDate(scope.from);
		result.setEndDate(scope.to);
		result.setStartBalance(ledger.getOpeningBalance());
		result.setEndBalance(ledger.getClosingBalance());
		result.setAccountStatementLine(lines);
		return result;

	}

	/** The first other account of the voucher, ie the bank account of a payment. */
	private static String otherAccount(AccountingVoucher v, String accountNo) {
		if (v.getLines() == null) return null;
		for (AccountingVoucherLine l : v.getLines()) {
			if (LedgerCalculator.isStruck(l)) continue;
			if (l.getAcctNo() != null && !l.getAcctNo().equals(accountNo)) return l.getAcctNo();
		}
		return null;
	}

	private static BigDecimal amount(double v) {
		return BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP);
	}

	/** The fiscal year period and date range of a report. */
	private static class Scope {
		AccountingPeriodData	data;
		/** The report period, within the fiscal year, with both dates set. */
		AccountingPeriod		period;
		LocalDate				from;
		LocalDate				to;
	}

	private Scope scope(BusinessPartner<?> bp, AccountingPeriod requested) throws Exception {

		TaxSubjectIdentifier tenant = new TaxSubjectIdentifier(bp);
		AccountingModelProvider provider = service.providerFor(null, tenant);
		if (provider == null) {
			throw new IllegalArgumentException("No accounting found for " + bp.getTaxId());
		}
		LocalDate start = requested != null ? requested.getPeriodStart() : null;
		LocalDate end   = requested != null ? requested.getPeriodEnd() : null;
		LocalDate ref = end != null ? end : start != null ? start : LocalDate.now();
		AccountingPeriod fy = service.fiscalYearOf(provider, tenant, ref);
		if (fy == null) {
			throw new IllegalArgumentException("No fiscal year of " + bp.getTaxId() + " contains " + ref);
		}
		AccountingPeriodData data = AccountingModelService.periodOf(service.model(provider, tenant, fy), fy);
		if (data == null) {
			throw new IllegalStateException("The accounting of " + bp.getTaxId() + " has no period " + fy.getName());
		}

		Scope scope = new Scope();
		scope.data = data;
		scope.from = start != null && start.isAfter(fy.getPeriodStart()) ? start : fy.getPeriodStart();
		scope.to   = end != null && end.isBefore(fy.getPeriodEnd()) ? end : fy.getPeriodEnd();
		scope.period = new AccountingPeriod();
		scope.period.setName(fy.getName());
		scope.period.setPeriodStart(scope.from);
		scope.period.setPeriodEnd(scope.to);
		return scope;

	}

}
