package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherRow;
import org.notima.businessobjects.adapter.fortnox.FortnoxVoucherSync;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.sync.AccountingVoucherSyncProfile;

/**
 * Conversion between Fortnox vouchers and canonical vouchers, without calling Fortnox.
 */
public class TestFortnoxVoucherSync {

	private static AccountingVoucher voucher() {
		AccountingVoucher v = new AccountingVoucher();
		v.setVoucherSeries("A");
		v.setVoucherNo("12");
		v.setAcctDate(LocalDate.of(2026, 3, 1));
		v.setDescription("Hyra mars");
		v.addVoucherLines(new BigDecimal("1000.50"), "5010", "1930");
		AccountingVoucherLine removed = new AccountingVoucherLine();
		removed.setAcctNo("6110");
		removed.setDebitAmount(new BigDecimal("5"));
		removed.setDeleted(true);
		v.addVoucherLine(removed);
		return v;
	}

	@Test
	public void testToFortnoxLeavesOutRemovedLines() throws Exception {
		Voucher f = FortnoxVoucherSync.toFortnox(voucher());
		assertEquals("A", f.getVoucherSeries());
		assertEquals("2026-03-01", f.getTransactionDate());
		assertEquals(2, f.getVoucherRows().getVoucherRow().size());
		assertNull("The number is assigned by Fortnox", f.getVoucherNumber());
	}

	@Test
	public void testRoundTripKeepsFingerprint() throws Exception {
		AccountingVoucher local = voucher();
		Voucher f = FortnoxVoucherSync.toFortnox(local);
		f.setVoucherNumber(12);
		// A row removed in Fortnox is read as a deleted line
		VoucherRow r = new VoucherRow();
		r.setAccount(6110);
		r.setDebit(5d);
		r.setRemoved(true);
		f.addVoucherRow(r);

		AccountingVoucher back = FortnoxVoucherSync.toCanonical(f);
		AccountingVoucherSyncProfile p = new AccountingVoucherSyncProfile();
		assertEquals(p.fingerprint(local), p.fingerprint(back));
		assertEquals(p.matchKey(local), p.matchKey(back));
		assertTrue(back.getLines().get(2).isDeleted());
	}

	@Test(expected = Exception.class)
	public void testLineWithoutAccountIsRejected() throws Exception {
		AccountingVoucher v = voucher();
		v.getLines().get(0).setAcctNo(null);
		FortnoxVoucherSync.toFortnox(v);
	}
}
