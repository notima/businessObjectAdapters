package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;

import org.junit.Test;
import org.notima.api.fortnox.entities3.Account;
import org.notima.businessobjects.adapter.fortnox.FortnoxAccountSync;
import org.notima.businessobjects.adapter.fortnox.FortnoxVatCodes;
import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.sync.AccountElementSyncProfile;
import org.notima.generic.businessobjects.tax.vatreport.VatReportDefinition;

/** Accounts to and from Fortnox: VAT codes as canonical tax keys, SRU as reporting code. */
public class TestFortnoxAccountSync {

	@Test
	public void fromFortnox() {
		AccountElement ae = FortnoxAccountSync.toCanonical(2650, "Redovisningskonto för moms", true, "R2", "7369");
		assertEquals("2650", ae.getAccountNo());
		assertEquals("Redovisningskonto för moms", ae.getName());
		assertEquals(VatReportDefinition.SETTLEMENT, ae.getTaxKey());
		assertEquals("7369", ae.getReportingCode());
		AccountElement none = FortnoxAccountSync.toCanonical(1930, "Bank", null, "", "0");
		assertNull(none.getTaxKey());
		assertNull(none.getReportingCode());
		assertTrue("no flag is active", none.isActive());
		assertFalse(FortnoxAccountSync.toCanonical(3999, "Gammalt", false, null, null).isActive());
	}

	@Test
	public void toFortnox() throws Exception {
		AccountElement ae = new AccountElement("2611");
		ae.setName("Utgående moms 25 %");
		ae.setTaxKey("10");
		Account a = FortnoxAccountSync.toFortnox(ae, null);
		assertEquals(Integer.valueOf(2611), a.getNumber());
		assertEquals("U1", a.getVATCode());
		assertNull("no SRU here leaves Fortnox's", a.getSRU());
		assertNull("balances are never written", a.getBalanceBroughtForward());
		assertEquals(Boolean.TRUE, a.getActive());
		ae.setActive(false);
		assertEquals(Boolean.FALSE, FortnoxAccountSync.toFortnox(ae, null).getActive());
		ae.setActive(true);

		ae.setTaxKey(null);
		assertEquals("no tax key clears the code", "", FortnoxAccountSync.toFortnox(ae, null).getVATCode());
	}

	@Test
	public void settlementCodeByAccountOrAsInFortnox() throws Exception {
		assertEquals("R1", FortnoxVatCodes.toVatCode(VatReportDefinition.SETTLEMENT, "1650"));
		assertEquals("R2", FortnoxVatCodes.toVatCode(VatReportDefinition.SETTLEMENT, "2650"));
		assertEquals("MP1", FortnoxVatCodes.toVatCode("MP1", "3001"));
		AccountElement ae = new AccountElement("2650");
		ae.setName("Momsfordran");
		ae.setTaxKey(VatReportDefinition.SETTLEMENT);
		Account existing = new Account();
		existing.setVATCode("R1");
		assertEquals("kept, as it gives the same key", "R1", FortnoxAccountSync.toFortnox(ae, existing).getVATCode());
	}

	@Test
	public void roundTripKeepsTheFingerprint() throws Exception {
		AccountElement remote = FortnoxAccountSync.toCanonical(3001, "Försäljning 25 %", false, "MP1", "7410");
		Account written = FortnoxAccountSync.toFortnox(remote, null);
		AccountElement back = FortnoxAccountSync.toCanonical(written.getNumber(), written.getDescription(), written.getActive(), written.getVATCode(), written.getSRU());
		AccountElementSyncProfile profile = new AccountElementSyncProfile();
		assertEquals(profile.fingerprint(remote), profile.fingerprint(back));
		assertFalse("inactive survives the round trip", back.isActive());
		assertEquals("2026-01-01/3001", FortnoxAccountSync.key(LocalDate.of(2026, 1, 1), 3001));
	}

	@Test(expected = Exception.class)
	public void notAnAccountNumber() throws Exception {
		AccountElement ae = new AccountElement("30011");
		ae.setName("x");
		FortnoxAccountSync.toFortnox(ae, null);
	}

}
