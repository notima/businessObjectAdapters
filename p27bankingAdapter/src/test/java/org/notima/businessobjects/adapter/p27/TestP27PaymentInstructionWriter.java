package org.notima.businessobjects.adapter.p27;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;
import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;

public class TestP27PaymentInstructionWriter {

	private static Payment<Object> payment(String id, String payee, String type, String account, String iban,
			double amount, String ref, String field, LocalDate date) {
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setName(payee);
		bp.setRemitToAccountType(type);
		bp.setRemitToAccount(account);
		bp.setRemitToIBAN(iban);
		Payment<Object> p = new Payment<Object>();
		p.setPaymentId(id);
		p.setCustomerPayment(false);
		p.setBusinessPartner(bp);
		p.setAmount(amount);
		p.setOriginalAmount(amount);
		p.setCurrency("SEK");
		p.setDestinationSystemReference(ref);
		p.setDestinationSystemReferenceField(field);
		p.setPaymentDate(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return p;
	}

	private static PaymentBatch batch() {
		PaymentBatch b = new PaymentBatch();
		BankAccountDetail ours = new BankAccountDetail();
		ours.setIBAN("SE4550000000058398257466");
		ours.setBIC("ESSESESS");
		ours.setCurrency("SEK");
		b.setBankAccount(ours);
		LocalDate d = LocalDate.of(2026, 10, 30);
		b.addPayment(payment("0b6f6c2e-1111-4a5b-9c1d-000000000001", "Leverantör Ett AB", "BG", "5050-1055", null, 1250, "1234567890", "OCR", d));
		b.addPayment(payment("0b6f6c2e-1111-4a5b-9c1d-000000000002", "Leverantör Två AB", "PG", "1234567", null, 99.5, "4711", "INVOICE", d));
		b.addPayment(payment("0b6f6c2e-1111-4a5b-9c1d-000000000003", "Lieferant GmbH", null, null, "DE89370400440532013000", 300, "RE-1", null, d.plusDays(1)));
		return b;
	}

	private static BusinessPartner<Object> company() {
		BusinessPartner<Object> c = new BusinessPartner<Object>();
		c.setName("Företaget AB");
		c.setTaxId("556000-0001");
		return c;
	}

	@Test
	public void testWritesPain001() throws Exception {
		P27PaymentInstructionWriter w = new P27PaymentInstructionWriter();
		assertTrue(w.validate(batch(), company()).toString(), w.validate(batch(), company()).isEmpty());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		w.writePaymentInstructions(batch(), company(), out);
		String xml = out.toString("UTF-8");
		assertTrue(xml.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.03"));
		assertTrue(xml.contains("<NbOfTxs>3</NbOfTxs>"));
		assertTrue(xml.contains("<CtrlSum>1649.50</CtrlSum>"));
		assertTrue(xml.contains("<Prtry>BGNR</Prtry>"));
		assertTrue(xml.contains("<MmbId>9960</MmbId>"));
		assertTrue(xml.contains("<Ref>1234567890</Ref>"));
		assertTrue(xml.contains("<Ustrd>4711</Ustrd>"));
		assertTrue(xml.contains("<IBAN>DE89370400440532013000</IBAN>"));
		assertTrue(xml.contains("<EndToEndId>0b6f6c2e11114a5b9c1d000000000001</EndToEndId>"));
		assertTrue(xml.contains("<Id>5560000001</Id>"));
		// Two execution dates: two groups
		assertEquals(2, xml.split("<PmtInf>", -1).length - 1);
	}

	@Test
	public void testPayeeWithoutAccountIsReported() {
		PaymentBatch b = batch();
		b.getPayments().get(1).getBusinessPartner().setRemitToAccountType(null);
		assertEquals(1, new P27PaymentInstructionWriter().validate(b, company()).size());
	}

}
