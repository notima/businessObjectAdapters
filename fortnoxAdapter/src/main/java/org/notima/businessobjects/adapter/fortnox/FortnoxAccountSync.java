package org.notima.businessobjects.adapter.fortnox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Account;
import org.notima.api.fortnox.entities3.AccountSubset;
import org.notima.api.fortnox.entities3.Accounts;
import org.notima.api.fortnox.entities3.FinancialYearSubset;
import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.sync.AccountElementSyncProfile;
import org.notima.generic.businessobjects.sync.RemoteObject;
import org.notima.generic.businessobjects.sync.SyncLink;
import org.notima.generic.businessobjects.sync.SyncOperation;
import org.notima.generic.businessobjects.sync.SyncProfile;
import org.notima.generic.businessobjects.sync.SyncQuery;
import org.notima.generic.ifacebusinessobjects.ObjectSyncHandler;

/**
 * Syncs a chart of accounts with the chart of the same financial year in Fortnox. The remote
 * key is {@code <first day of the financial year>/<account number>}.
 *
 * <ul>
 *   <li>Compared and written ({@link AccountElementSyncProfile}): the name (Fortnox's
 *       description), whether the account is active, the VAT code, translated to and from the canonical tax keys by
 *       {@link FortnoxVatCodes}, and the SRU code as the reporting code.</li>
 *   <li>The listing of the chart has all that is compared, so the whole chart is listed (one
 *       call per page) and the linked accounts are compared too: changes made in Fortnox are
 *       found without fetching each account.</li>
 *   <li>Inactive accounts in Fortnox are left out of the listing unless linked, so that the
 *       unused part of Fortnox's chart isn't offered to be fetched. An account created here
 *       that Fortnox already has as inactive is given the content from here, and so activated
 *       if active here.</li>
 *   <li>When updating, the VAT code in Fortnox is kept if it gives the same tax key (ie R1/R2),
 *       and a missing SRU code here leaves Fortnox's code as it is.</li>
 *   <li>Balances are never written: they come with the vouchers.</li>
 * </ul>
 *
 * <p>The financial year is that of the query's first day. Accounts are created in the financial
 * year last listed for the company, as the engine lists before it carries out a plan.</p>
 */
public class FortnoxAccountSync implements ObjectSyncHandler<AccountElement> {

	public static final String REGISTER = "account";

	private final FortnoxConnector connector;
	private final AccountElementSyncProfile profile = new AccountElementSyncProfile();
	/** The financial year last listed, per org no. */
	private final Map<String, FinancialYearSubset> listedYears = new ConcurrentHashMap<String, FinancialYearSubset>();
	/** Financial year id per org no and first day of the year. */
	private final Map<String, Integer> yearIds = new ConcurrentHashMap<String, Integer>();

	public FortnoxAccountSync(FortnoxConnector connector) {
		this.connector = connector;
	}

	@Override
	public Class<AccountElement> getType() {
		return AccountElement.class;
	}

	@Override
	public String getRegister() {
		return REGISTER;
	}

	@Override
	public Set<SyncOperation> getSupportedOperations() {
		return Collections.unmodifiableSet(EnumSet.of(SyncOperation.READ, SyncOperation.CREATE, SyncOperation.UPDATE));
	}

	@Override
	public SyncProfile<AccountElement> getProfile() {
		return profile;
	}

	@Override
	public List<RemoteObject<AccountElement>> list(String orgNo, SyncQuery query) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		LocalDate from = query.getFromDate() != null ? query.getFromDate() : LocalDate.now();
		FinancialYearSubset fy = client.getFinancialYear(java.sql.Date.valueOf(from));
		if (fy == null) throw new Exception("No financial year in Fortnox for " + from);
		LocalDate fyStart = LocalDate.parse(fy.getFromDate());
		LocalDate fyEnd = LocalDate.parse(fy.getToDate());
		if (query.getToDate() != null && query.getToDate().isAfter(fyEnd)) {
			throw new Exception("The period " + from + " - " + query.getToDate() + " isn't within one financial year in Fortnox ("
					+ fy.getFromDate() + " - " + fy.getToDate() + ")");
		}
		listedYears.put(orgNo, fy);
		yearIds.put(orgNo + "|" + fyStart, fy.getId());

		List<RemoteObject<AccountElement>> result = new ArrayList<RemoteObject<AccountElement>>();
		Accounts accounts = client.getAccounts(fy.getId());
		if (accounts == null || accounts.getAccountSubset() == null) return result;
		for (AccountSubset a : accounts.getAccountSubset()) {
			if (a.getNumber() == null) continue;
			String key = key(fyStart, a.getNumber());
			if (Boolean.FALSE.equals(a.getActive()) && !query.getLinkedRemoteKeys().contains(key)) continue;
			result.add(new RemoteObject<AccountElement>(key,
					toCanonical(a.getNumber(), a.getDescription(), a.getActive(), a.getVATCode(), a.getSRU()), true));
		}
		return result;
	}

	@Override
	public AccountElement fetch(String orgNo, String remoteKey) throws Exception {
		Key k = parseKey(remoteKey);
		FortnoxClient3 client = connector.client(orgNo);
		Account a = client.getAccount(yearId(client, orgNo, k.fyStart), k.accountNo);
		if (a == null) throw new Exception("Account " + remoteKey + " not found in Fortnox");
		return toCanonical(a.getNumber(), a.getDescription(), a.getActive(), a.getVATCode(), a.getSRU());
	}

	/** Creates the account in the financial year last listed, or writes over one Fortnox has as inactive. */
	@Override
	public SyncLink create(String orgNo, AccountElement obj) throws Exception {
		FinancialYearSubset fy = listedYears.get(orgNo);
		if (fy == null) throw new Exception("No financial year chosen in Fortnox: list the accounts first");
		LocalDate fyStart = LocalDate.parse(fy.getFromDate());
		FortnoxClient3 client = connector.client(orgNo);
		int no = accountNo(obj);
		Account existing = client.getAccount(fy.getId(), no);
		Account a = toFortnox(obj, existing);
		Account written = client.setAccount(fy.getId(), a, existing == null);
		if (written == null || written.getNumber() == null) throw new Exception("Fortnox didn't return the account");
		return new SyncLink(FortnoxAdapter.SYSTEMNAME, key(fyStart, written.getNumber()));
	}

	@Override
	public SyncLink update(String orgNo, AccountElement obj, SyncLink link) throws Exception {
		Key k = parseKey(link.getRemoteKey());
		FortnoxClient3 client = connector.client(orgNo);
		int yearId = yearId(client, orgNo, k.fyStart);
		Account existing = client.getAccount(yearId, k.accountNo);
		if (existing == null) throw new Exception("Account " + link.getRemoteKey() + " not found in Fortnox");
		if (accountNo(obj) != k.accountNo) {
			throw new Exception("The account number has changed from " + k.accountNo + " to " + obj.getAccountNo()
					+ ": an account's number can't be changed in Fortnox");
		}
		client.setAccount(yearId, toFortnox(obj, existing), false);
		return link;
	}

	// ------------------------------------------------------------------

	public static AccountElement toCanonical(Integer number, String description, Boolean active, String vatCode, String sru) {
		AccountElement ae = new AccountElement(String.valueOf(number));
		ae.setName(FortnoxArticleSync.empty(description));
		ae.setActive(!Boolean.FALSE.equals(active));
		ae.setTaxKey(FortnoxVatCodes.toTaxKey(vatCode));
		String code = FortnoxArticleSync.empty(sru);
		ae.setReportingCode(code != null && !"0".equals(code) ? code : null);
		return ae;
	}

	/**
	 * The account to write: the number, description, whether active, VAT code and SRU code.
	 *
	 * @param existing	The account in Fortnox, or null if new: its VAT code is kept when it gives
	 * 					the same tax key.
	 */
	public static Account toFortnox(AccountElement ae, Account existing) throws Exception {
		Account a = new Account();
		a.setNumber(accountNo(ae));
		String name = FortnoxArticleSync.empty(ae.getName());
		if (name == null) throw new Exception("Account " + ae.getAccountNo() + " has no name");
		a.setDescription(name);
		a.setActive(ae.isActive());
		String taxKey = FortnoxArticleSync.empty(ae.getTaxKey());
		if (taxKey == null) {
			a.setVATCode("");
		} else if (existing != null && existing.getVATCode() != null
				&& taxKey.equalsIgnoreCase(String.valueOf(FortnoxVatCodes.toTaxKey(existing.getVATCode())))) {
			a.setVATCode(existing.getVATCode().trim());
		} else {
			a.setVATCode(FortnoxVatCodes.toVatCode(taxKey, ae.getAccountNo()));
		}
		String sru = FortnoxArticleSync.empty(ae.getReportingCode());
		if (sru != null && !"0".equals(sru)) {
			if (!sru.matches("\\d+")) throw new Exception("The SRU code of account " + ae.getAccountNo() + " isn't a number: " + sru);
			a.setSRU(sru);
		}
		return a;
	}

	private static int accountNo(AccountElement ae) throws Exception {
		String no = FortnoxArticleSync.empty(ae.getAccountNo());
		if (no == null || !no.matches("\\d{4}")) throw new Exception("Not an account number in Fortnox (four digits): " + ae.getAccountNo());
		return Integer.parseInt(no);
	}

	public static String key(LocalDate fyStart, Integer accountNo) {
		return fyStart + "/" + accountNo;
	}

	private static final class Key {
		final LocalDate fyStart;
		final int accountNo;
		Key(LocalDate fyStart, int accountNo) {
			this.fyStart = fyStart;
			this.accountNo = accountNo;
		}
	}

	private static Key parseKey(String remoteKey) throws Exception {
		String[] parts = remoteKey != null ? remoteKey.split("/", 2) : new String[0];
		try {
			if (parts.length == 2) return new Key(LocalDate.parse(parts[0]), Integer.parseInt(parts[1]));
		} catch (RuntimeException e) {
			// fall through
		}
		throw new Exception("Invalid Fortnox account key: " + remoteKey);
	}

	private int yearId(FortnoxClient3 client, String orgNo, LocalDate fyStart) throws Exception {
		Integer id = yearIds.get(orgNo + "|" + fyStart);
		if (id != null) return id;
		FinancialYearSubset fy = client.getFinancialYear(java.sql.Date.valueOf(fyStart));
		if (fy == null) throw new Exception("No financial year in Fortnox starting " + fyStart);
		yearIds.put(orgNo + "|" + fyStart, fy.getId());
		return fy.getId();
	}

}
