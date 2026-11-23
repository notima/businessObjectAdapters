package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.PayeeAccount;
import org.notima.generic.businessobjects.PayeeAccountType;
import org.notima.generic.businessobjects.PayeeAccounts;

/**
 * Where to pay a Fortnox supplier, as {@link PayeeAccount}s: Fortnox has one of each of
 * bankgiro ({@code BG}), plusgiro ({@code PG}), bank account ({@code BankAccountNumber} with
 * {@code ClearingNumber} and {@code Bank}) and {@code IBAN} with {@code BIC}.
 *
 * <ul>
 *   <li>{@link #toPartner} gives the partner the accounts (bankgiro preferred, then plusgiro,
 *       bank account and IBAN), and sets its remit-to fields as Fortnox has them.</li>
 *   <li>{@link #fromPartner} picks, of the partner's active accounts with the preferred first,
 *       the first of each kind. A bank account is one in Sweden (or without country).</li>
 * </ul>
 */
public final class FortnoxPayeeAccounts {

	/** One of each of what Fortnox stores; null where there is none. */
	public static final class Fields {
		public String bg;
		public String pg;
		public String bankAccountNo;
		public String clearingNo;
		public String bank;
		public String iban;
		public String bic;
		/**
		 * Whether the partner told its bank account: false for a partner with only remit-to
		 * fields, which can't, so that an update leaves the bank account in Fortnox as it is.
		 */
		public boolean bankAccountKnown;
	}

	private FortnoxPayeeAccounts() {}

	/**
	 * Sets the accounts of a supplier read from Fortnox on the partner, and its remit-to fields.
	 *
	 * @param country	The supplier's country, for the bank account; Sweden if null.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void toPartner(BusinessPartner<?> bp, Fields f, String country) {
		List<PayeeAccount> accounts = new ArrayList<PayeeAccount>();
		if (f.bg != null) accounts.add(new PayeeAccount(PayeeAccountType.BANKGIRO_CODE, f.bg));
		if (f.pg != null) accounts.add(new PayeeAccount(PayeeAccountType.PLUSGIRO_CODE, f.pg));
		if (f.bankAccountNo != null) {
			PayeeAccount a = new PayeeAccount(PayeeAccountType.BANK_CODE, f.bankAccountNo);
			a.setCountryCode(country != null ? country.trim().toUpperCase() : "SE");
			a.setBankCode(f.clearingNo);
			a.setBankName(f.bank);
			accounts.add(a);
		}
		if (f.iban != null) {
			PayeeAccount a = new PayeeAccount(PayeeAccountType.IBAN_CODE, f.iban);
			a.setBic(f.bic);
			accounts.add(a);
		}
		if (!accounts.isEmpty()) accounts.get(0).setPreferred(true);
		((BusinessPartner) bp).setPayeeAccounts(accounts.isEmpty() ? null : accounts);
		// As Fortnox has them, as before there were payee accounts
		bp.setRemitToAccountType(f.bg != null ? PayeeAccountType.BANKGIRO_CODE : f.pg != null ? PayeeAccountType.PLUSGIRO_CODE : null);
		bp.setRemitToAccount(f.bg != null ? f.bg : f.pg);
		bp.setRemitToIBAN(f.iban);
		bp.setRemitToBIC(f.bic);
	}

	/** What to store in Fortnox for a partner: its accounts, or its remit-to fields if it has none. */
	public static Fields fromPartner(BusinessPartner<?> bp) {
		Fields f = new Fields();
		f.bankAccountKnown = bp.getPayeeAccounts() != null && !bp.getPayeeAccounts().isEmpty();
		List<PayeeAccount> ordered = preferredFirst(PayeeAccounts.of(bp));
		PayeeAccount bg = first(ordered, PayeeAccountType.BANKGIRO_CODE);
		PayeeAccount pg = first(ordered, PayeeAccountType.PLUSGIRO_CODE);
		PayeeAccount iban = first(ordered, PayeeAccountType.IBAN_CODE);
		PayeeAccount bank = null;
		for (PayeeAccount a : ordered) {
			if (a.isType(PayeeAccountType.BANK_CODE) && notBlank(a.getAccountNo())
					&& (!notBlank(a.getCountryCode()) || "SE".equalsIgnoreCase(a.getCountryCode().trim()))) {
				bank = a;
				break;
			}
		}
		f.bg = bg != null ? bg.getAccountNo().trim() : null;
		f.pg = pg != null ? pg.getAccountNo().trim() : null;
		if (bank != null) {
			f.bankAccountNo = bank.getAccountNo().replaceAll("[\\s\\-]", "");
			f.clearingNo = notBlank(bank.getBankCode()) ? bank.getBankCode().replaceAll("[^0-9]", "") : null;
			f.bank = notBlank(bank.getBankName()) ? bank.getBankName().trim() : null;
		}
		if (iban != null) {
			f.iban = PayeeAccountType.IBAN.normalize(iban.getAccountNo());
			f.bic = notBlank(iban.getBic()) ? iban.getBic().replaceAll("\\s", "").toUpperCase() : null;
		}
		return f;
	}

	/** The active accounts, the preferred first. */
	private static List<PayeeAccount> preferredFirst(List<PayeeAccount> accounts) {
		List<PayeeAccount> list = new ArrayList<PayeeAccount>();
		for (PayeeAccount a : accounts) if (a.isActive() && a.isPreferred()) list.add(a);
		for (PayeeAccount a : accounts) if (a.isActive() && !a.isPreferred()) list.add(a);
		return list;
	}

	private static PayeeAccount first(List<PayeeAccount> accounts, String type) {
		for (PayeeAccount a : accounts) if (a.isType(type) && notBlank(a.getAccountNo())) return a;
		return null;
	}

	private static boolean notBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}

}
