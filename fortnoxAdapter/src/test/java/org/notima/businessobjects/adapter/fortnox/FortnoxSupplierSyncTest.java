package org.notima.businessobjects.adapter.fortnox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.notima.api.fortnox.entities3.Supplier;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.PayeeAccount;

/** How where to pay a supplier is mapped to and from Fortnox. */
public class FortnoxSupplierSyncTest {

	@Test
	public void testFromFortnox() {
		Supplier s = new Supplier();
		s.setName("Leverantören AB");
		s.setCountryCode("SE");
		s.setBG("5050-1055");
		s.setBankAccountNumber("1234567890");
		s.setClearingNumber("8327-9");
		s.setBank("Swedbank");
		s.setIBAN("SE4550000000058398257466");
		s.setBIC("ESSESESS");
		BusinessPartner<Supplier> bp = FortnoxSupplierSync.toCanonical(s);

		assertEquals(3, bp.getPayeeAccounts().size());
		assertTrue(bp.getPayeeAccounts().get(0).isType("BG"));
		assertTrue(bp.getPayeeAccounts().get(0).isPreferred());
		PayeeAccount bank = bp.getPayeeAccounts().get(1);
		assertTrue(bank.isType("BANK"));
		assertEquals("1234567890", bank.getAccountNo());
		assertEquals("8327-9", bank.getBankCode());
		assertEquals("Swedbank", bank.getBankName());
		assertEquals("SE", bank.getCountryCode());
		assertEquals("ESSESESS", bp.getPayeeAccounts().get(2).getBic());
		// The remit-to fields as before
		assertEquals("BG", bp.getRemitToAccountType());
		assertEquals("5050-1055", bp.getRemitToAccount());
		assertEquals("SE4550000000058398257466", bp.getRemitToIBAN());
	}

	@Test
	public void testToFortnox() throws Exception {
		BusinessPartner<Object> bp = partner();
		PayeeAccount pg = new PayeeAccount("PG", "44769-8");
		PayeeAccount bank = new PayeeAccount("BANK", "123 456 789-0");
		bank.setCountryCode("SE");
		bank.setBankCode("8327-9");
		bank.setBankName("Swedbank");
		bank.setPreferred(true);
		PayeeAccount foreign = new PayeeAccount("BANK", "31926819");
		foreign.setCountryCode("GB");
		bp.setPayeeAccounts(new ArrayList<PayeeAccount>(Arrays.asList(pg, foreign, bank)));

		Supplier s = FortnoxSupplierSync.toFortnox(bp, true);
		assertEquals("44769-8", s.getPG());
		assertEquals("1234567890", s.getBankAccountNumber());
		assertEquals("83279", s.getClearingNumber());
		assertEquals("Swedbank", s.getBank());
		// Cleared in Fortnox: the partner has none
		assertEquals("", s.getBG());
		assertEquals("", s.getIBAN());
	}

	@Test
	public void testPartnerWithOnlyRemitToFieldsLeavesTheBankAccount() throws Exception {
		BusinessPartner<Object> bp = partner();
		bp.setRemitToAccountType("BG");
		bp.setRemitToAccount("5050-1055");
		Supplier s = FortnoxSupplierSync.toFortnox(bp, true);
		assertEquals("5050-1055", s.getBG());
		assertNull(s.getBankAccountNumber());
		assertNull(s.getClearingNumber());
		assertNull(s.getBank());
	}

	private static BusinessPartner<Object> partner() {
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setName("Leverantören AB");
		bp.setCountryCode("SE");
		return bp;
	}

}
