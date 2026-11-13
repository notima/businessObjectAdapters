package org.notima.businessobjects.adapter.fortnox;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.clients.FortnoxClientInfo;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.api.fortnox.entities3.AccountSubset;
import org.notima.api.fortnox.entities3.Accounts;
import org.notima.api.fortnox.entities3.FinancialYearSubset;
import org.notima.api.fortnox.entities3.FinancialYears;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;
import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.AccountingFileData;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.AccountingFileImporter;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;
import org.notima.util.LocalDateUtils;

/**
 * Provides the accounting of Fortnox clients: their financial years, and per year a SIE 4
 * export from Fortnox, read by the registered SIE {@link AccountingFileImporter}.
 *
 * <p>Uses a client of its own for each call, so it doesn't change the tenant selected in the
 * {@link FortnoxAdapter}.</p>
 */
public class FortnoxAccountingModelProvider implements AccountingModelProvider {

	/** The format name of the importer used for the SIE exports. */
	public static final String SIE_FORMAT = "SIE4";

	private final FortnoxClientManager clientManager;
	private final Supplier<AccountingFileImporter> sieImporter;

	/**
	 * @param clientManager	The Fortnox clients.
	 * @param sieImporter	Gives the SIE importer when needed; may give null if none is available.
	 */
	public FortnoxAccountingModelProvider(FortnoxClientManager clientManager, Supplier<AccountingFileImporter> sieImporter) {
		this.clientManager = clientManager;
		this.sieImporter = sieImporter;
	}

	@Override
	public String getSystemName() {
		return FortnoxAdapter.SYSTEMNAME;
	}

	@Override
	public boolean hasAccountingFor(TaxSubjectIdentifier tenant) {
		return clientInfo(tenant) != null;
	}

	@Override
	public List<AccountingPeriod> listFiscalYears(TaxSubjectIdentifier tenant) throws Exception {
		List<AccountingPeriod> result = new ArrayList<AccountingPeriod>();
		FinancialYears years = client(tenant).getFinancialYears();
		if (years == null || years.getFinancialYearSubset() == null) return result;
		for (FinancialYearSubset fy : years.getFinancialYearSubset()) {
			if (fy.getFromDate() == null || fy.getToDate() == null) continue;
			AccountingPeriod p = new AccountingPeriod();
			p.setPeriodStart(LocalDate.parse(fy.getFromDate()));
			p.setPeriodEnd(LocalDate.parse(fy.getToDate()));
			p.setName(name(p));
			result.add(p);
		}
		result.sort(Comparator.comparing(AccountingPeriod::getPeriodStart));
		return result;
	}

	@Override
	public AccountingFileData getAccountingModel(TaxSubjectIdentifier tenant, AccountingPeriod fiscalYear) throws Exception {
		AccountingFileImporter importer = sieImporter != null ? sieImporter.get() : null;
		if (importer == null) {
			throw new IllegalStateException("No " + SIE_FORMAT + " importer available, is the SIE adapter installed?");
		}
		FortnoxClient3 client = client(tenant);
		FinancialYearSubset fy = client.getFinancialYear(LocalDateUtils.asDate(fiscalYear.getPeriodStart()));
		if (fy == null || fy.getId() == null) return null;
		ByteBuffer sie = client.retrieveSieFile(4, fy.getId());
		if (sie == null) return null;
		byte[] bytes = new byte[sie.remaining()];
		sie.duplicate().get(bytes);
		AccountingFileData data = importer.importStream(new ByteArrayInputStream(bytes), "fortnox-" + fy.getId() + ".se");
		// The SIE export has no VAT codes: take them from the chart of accounts of the year
		try {
			applyVatCodes(data, client.getAccounts(fy.getId()));
		} catch (Exception e) {
			FortnoxAdapter.logger.warn("Could not read the VAT codes of the accounts of " + tenant.getTaxId() + ": " + e.getMessage());
		}
		return data;
	}

	/**
	 * Sets the VAT code of each account in Fortnox, translated to the tax keys of the canonical
	 * model ({@link FortnoxVatCodes}), as the tax key of the account in the model
	 * ({@link AccountElement#getTaxKey()}), where the model has none, ie for the VAT return.
	 * Likewise the SRU code of each account as its reporting code
	 * ({@link AccountElement#getReportingCode()}), for the corporate income tax.
	 */
	static void applyVatCodes(AccountingFileData data, Accounts accounts) {
		if (data == null || data.getPeriods() == null || accounts == null || accounts.getAccountSubset() == null) return;
		Map<String, String> codes = new HashMap<String, String>();
		Map<String, String> sruCodes = new HashMap<String, String>();
		for (AccountSubset a : accounts.getAccountSubset()) {
			if (a.getNumber() == null) continue;
			if (a.getSRU() != null && !a.getSRU().trim().isEmpty() && !"0".equals(a.getSRU().trim())) {
				sruCodes.put(a.getNumber().toString(), a.getSRU().trim());
			}
			if (a.getVATCode() == null || a.getVATCode().trim().isEmpty()) continue;
			codes.put(a.getNumber().toString(), FortnoxVatCodes.toTaxKey(a.getVATCode()));
		}
		for (AccountingPeriodData p : data.getPeriods()) {
			if (p.getChartOfAccounts() == null) continue;
			for (AccountElement ae : p.getChartOfAccounts()) {
				if (ae.getAccountNo() == null) continue;
				if (ae.getReportingCode() == null || ae.getReportingCode().trim().isEmpty()) {
					String sru = sruCodes.get(ae.getAccountNo());
					if (sru != null) ae.setReportingCode(sru);
				}
				if (ae.getTaxKey() != null && !ae.getTaxKey().trim().isEmpty()) continue;
				String code = codes.get(ae.getAccountNo());
				if (code != null) ae.setTaxKey(code);
			}
		}
	}

	/** The client info of the tenant, by its org number as given or as NNNNNN-NNNN. */
	private FortnoxClientInfo clientInfo(TaxSubjectIdentifier tenant) {
		if (clientManager == null || tenant == null || tenant.getTaxId() == null) return null;
		String taxId = tenant.getTaxId().trim();
		FortnoxClientInfo info = clientManager.getClientInfoByOrgNo(taxId);
		if (info == null) {
			String digits = taxId.replaceAll("[^0-9]", "");
			if (digits.length() == 10) {
				info = clientManager.getClientInfoByOrgNo(digits.substring(0, 6) + "-" + digits.substring(6));
			}
		}
		return info;
	}

	private FortnoxClient3 client(TaxSubjectIdentifier tenant) throws Exception {
		FortnoxClientInfo info = clientInfo(tenant);
		if (info == null) {
			throw new IllegalArgumentException("Not a Fortnox client: " + tenant.getTaxId());
		}
		return new FortnoxClient3(new FileCredentialsProvider(info.getOrgNo()));
	}

	/** "2025" for a calendar year, else "2025-2026". */
	private static String name(AccountingPeriod p) {
		int s = p.getPeriodStart().getYear();
		int e = p.getPeriodEnd().getYear();
		return s == e ? String.valueOf(s) : s + "-" + e;
	}

}
