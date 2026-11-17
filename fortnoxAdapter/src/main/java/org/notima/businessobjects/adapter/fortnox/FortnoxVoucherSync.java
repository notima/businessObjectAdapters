package org.notima.businessobjects.adapter.fortnox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.FinancialYearSubset;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherRow;
import org.notima.api.fortnox.entities3.VoucherSubset;
import org.notima.api.fortnox.entities3.Vouchers;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.sync.AccountingVoucherSyncProfile;
import org.notima.generic.businessobjects.sync.RemoteObject;
import org.notima.generic.businessobjects.sync.SyncLink;
import org.notima.generic.businessobjects.sync.SyncOperation;
import org.notima.generic.businessobjects.sync.SyncProfile;
import org.notima.generic.businessobjects.sync.SyncQuery;
import org.notima.generic.ifacebusinessobjects.ObjectSyncHandler;

/**
 * Syncs vouchers with Fortnox.
 *
 * <p>Fortnox vouchers can be read and created, not changed (corrections are made in Fortnox, or
 * with a new voucher). The remote key is {@code <first day of financial year>/<series>/<number>},
 * ie {@code 2026-01-01/A/12}. The listing returns summaries (series, number, date, description)
 * of the vouchers not already linked; lines are fetched only when needed.</p>
 *
 * <p>A voucher is created in the series it has here. The series must exist in Fortnox, and
 * Fortnox assigns the number: the local voucher gets that number.</p>
 */
public class FortnoxVoucherSync implements ObjectSyncHandler<AccountingVoucher> {

	private final FortnoxConnector connector;
	private final SyncProfile<AccountingVoucher> profile = new AccountingVoucherSyncProfile();
	/** Financial year id per org no and first day of the year. */
	private final Map<String, Integer> yearIds = new ConcurrentHashMap<String, Integer>();

	public FortnoxVoucherSync(FortnoxConnector connector) {
		this.connector = connector;
	}

	@Override
	public Class<AccountingVoucher> getType() {
		return AccountingVoucher.class;
	}

	@Override
	public Set<SyncOperation> getSupportedOperations() {
		return Collections.unmodifiableSet(EnumSet.of(SyncOperation.READ, SyncOperation.CREATE));
	}

	@Override
	public SyncProfile<AccountingVoucher> getProfile() {
		return profile;
	}

	@Override
	public List<RemoteObject<AccountingVoucher>> list(String orgNo, SyncQuery query) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		LocalDate from = query.getFromDate()!=null ? query.getFromDate() : LocalDate.now();
		FinancialYearSubset fy = client.getFinancialYear(java.sql.Date.valueOf(from));
		if (fy==null) throw new Exception("No financial year in Fortnox for " + from);
		LocalDate fyStart = LocalDate.parse(fy.getFromDate());
		LocalDate fyEnd = LocalDate.parse(fy.getToDate());
		if (query.getToDate()!=null && query.getToDate().isAfter(fyEnd)) {
			throw new Exception("The period " + from + " - " + query.getToDate() + " isn't within one financial year in Fortnox ("
					+ fy.getFromDate() + " - " + fy.getToDate() + ")");
		}
		yearIds.put(orgNo + "|" + fyStart, fy.getId());

		List<RemoteObject<AccountingVoucher>> result = new ArrayList<RemoteObject<AccountingVoucher>>();
		Vouchers vouchers = client.getVouchers(fy.getId(), null);
		if (vouchers==null || vouchers.getVoucherSubset()==null) return result;
		for (VoucherSubset vs : vouchers.getVoucherSubset()) {
			if (vs.getVoucherNumber()==null || vs.getVoucherSeries()==null) continue;
			String key = key(fyStart, vs.getVoucherSeries(), vs.getVoucherNumber());
			if (query.getLinkedRemoteKeys().contains(key)) continue;
			LocalDate date = vs.getTransactionDate()!=null ? LocalDate.parse(vs.getTransactionDate()) : null;
			if (date!=null && (date.isBefore(from) || (query.getToDate()!=null && date.isAfter(query.getToDate())))) continue;
			AccountingVoucher summary = new AccountingVoucher();
			summary.setVoucherSeries(vs.getVoucherSeries());
			summary.setVoucherNo(String.valueOf(vs.getVoucherNumber()));
			summary.setAcctDate(date);
			summary.setDescription(vs.getDescription());
			result.add(new RemoteObject<AccountingVoucher>(key, summary, false));
		}
		return result;
	}

	@Override
	public AccountingVoucher fetch(String orgNo, String remoteKey) throws Exception {
		String[] parts = parseKey(remoteKey);
		LocalDate fyStart = LocalDate.parse(parts[0]);
		FortnoxClient3 client = connector.client(orgNo);
		Voucher v = client.getVoucher(yearId(client, orgNo, fyStart), parts[1], Integer.parseInt(parts[2]));
		if (v==null) throw new Exception("Voucher " + remoteKey + " not found in Fortnox");
		return toCanonical(v);
	}

	@Override
	public SyncLink create(String orgNo, AccountingVoucher obj) throws Exception {
		if (obj.getVoucherSeries()==null || obj.getVoucherSeries().trim().isEmpty()) {
			throw new Exception("The voucher has no series");
		}
		if (obj.getAcctDate()==null) throw new Exception("The voucher has no date");
		FortnoxClient3 client = connector.client(orgNo);
		FinancialYearSubset fy = client.getFinancialYear(java.sql.Date.valueOf(obj.getAcctDate()));
		if (fy==null) throw new Exception("No financial year in Fortnox for " + obj.getAcctDate());
		LocalDate fyStart = LocalDate.parse(fy.getFromDate());
		yearIds.put(orgNo + "|" + fyStart, fy.getId());

		Voucher created = client.setVoucher(toFortnox(obj), fy.getId());
		if (created==null || created.getVoucherNumber()==null) {
			throw new Exception("Fortnox didn't return a voucher number");
		}
		obj.setVoucherSeries(created.getVoucherSeries());
		obj.setVoucherNo(String.valueOf(created.getVoucherNumber()));
		return new SyncLink(FortnoxAdapter.SYSTEMNAME, key(fyStart, created.getVoucherSeries(), created.getVoucherNumber()));
	}

	// ------------------------------------------------------------------

	static String key(LocalDate fyStart, String series, Integer number) {
		return fyStart + "/" + series + "/" + number;
	}

	private static String[] parseKey(String remoteKey) throws Exception {
		String[] parts = remoteKey!=null ? remoteKey.split("/", 3) : new String[0];
		if (parts.length!=3) throw new Exception("Invalid Fortnox voucher key: " + remoteKey);
		return parts;
	}

	private int yearId(FortnoxClient3 client, String orgNo, LocalDate fyStart) throws Exception {
		Integer id = yearIds.get(orgNo + "|" + fyStart);
		if (id!=null) return id;
		FinancialYearSubset fy = client.getFinancialYear(java.sql.Date.valueOf(fyStart));
		if (fy==null) throw new Exception("No financial year in Fortnox starting " + fyStart);
		yearIds.put(orgNo + "|" + fyStart, fy.getId());
		return fy.getId();
	}

	public static AccountingVoucher toCanonical(Voucher v) {
		AccountingVoucher dst = new AccountingVoucher();
		dst.setVoucherSeries(v.getVoucherSeries());
		dst.setVoucherNo(v.getVoucherNumber()!=null ? String.valueOf(v.getVoucherNumber()) : null);
		dst.setAcctDate(v.getTransactionDate()!=null ? LocalDate.parse(v.getTransactionDate()) : null);
		dst.setDescription(v.getDescription());
		dst.setCostCenter(empty(v.getCostCenter()));
		dst.setProjectCode(empty(v.getProject()));
		if (v.getVoucherRows()!=null && v.getVoucherRows().getVoucherRow()!=null) {
			for (VoucherRow r : v.getVoucherRows().getVoucherRow()) {
				AccountingVoucherLine l = new AccountingVoucherLine();
				l.setAcctNo(r.getAccount()!=null ? String.valueOf(r.getAccount()) : null);
				l.setDebitAmount(amount(r.getDebit()));
				l.setCreditAmount(amount(r.getCredit()));
				l.setCostCenter(empty(r.getCostCenter()));
				l.setProjectCode(empty(r.getProject()));
				l.setDescription(empty(r.getTransactionInformation()));
				if (Boolean.TRUE.equals(r.getRemoved())) l.setDeleted(true);
				dst.addVoucherLine(l);
			}
		}
		return dst;
	}

	public static Voucher toFortnox(AccountingVoucher src) throws Exception {
		Voucher dst = new Voucher();
		dst.setVoucherSeries(src.getVoucherSeries().trim());
		dst.setTransactionDate(src.getAcctDate().toString());
		dst.setDescription(src.getDescription()!=null && !src.getDescription().trim().isEmpty() ? src.getDescription().trim() : "-");
		dst.setCostCenter(empty(src.getCostCenter()));
		dst.setProject(empty(src.getProjectCode()));
		int rows = 0;
		if (src.getLines()!=null) {
			for (AccountingVoucherLine l : src.getLines()) {
				if (Boolean.TRUE.equals(l.isDeleted())) continue;
				if (l.getAcctNo()==null || !l.getAcctNo().trim().matches("\\d+")) {
					throw new Exception("A line has no valid account number: " + l.getAcctNo());
				}
				VoucherRow r = new VoucherRow();
				r.setAccount(Integer.parseInt(l.getAcctNo().trim()));
				r.setDebit(nz(l.getDebitAmount()).doubleValue());
				r.setCredit(nz(l.getCreditAmount()).doubleValue());
				r.setCostCenter(empty(l.getCostCenter()));
				r.setProject(empty(l.getProjectCode()));
				if (l.getDescription()!=null && !l.getDescription().trim().isEmpty()) {
					r.setTransactionInformation(l.getDescription().trim());
				}
				dst.addVoucherRow(r);
				rows++;
			}
		}
		if (rows==0) throw new Exception("The voucher has no lines");
		return dst;
	}

	private static BigDecimal amount(Double d) {
		return d!=null ? BigDecimal.valueOf(d).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

	private static BigDecimal nz(BigDecimal b) {
		return b!=null ? b : BigDecimal.ZERO;
	}

	private static String empty(String s) {
		return s!=null && !s.trim().isEmpty() ? s.trim() : null;
	}

}
