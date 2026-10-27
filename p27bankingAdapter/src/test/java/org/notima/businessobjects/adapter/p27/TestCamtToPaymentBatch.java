package org.notima.businessobjects.adapter.p27;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.Payment.PaymentType;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.iso20022.camt.CamtAccountReport;
import org.notima.iso20022.camt.CamtDocument;
import org.notima.iso20022.camt.CamtReader;

public class TestCamtToPaymentBatch {

	private static final String EXAMPLE_DIR = "src/test/resources/camt";
	// Real bank files, not in version control. Tests on them are skipped if missing.
	private static final String REAL_FILES_DIR = "test-files";

	@Test
	public void testExampleStatement() throws Exception {

		P27PaymentBatchFactory factory = new P27PaymentBatchFactory(EXAMPLE_DIR);
		List<PaymentBatch> batches = factory.createPaymentBatchesFromFile("camt053_v02_example.xml");
		assertEquals(2, batches.size());

		PaymentBatch in = batches.get(0);
		assertEquals(PaymentType.RECEIVABLE, in.getPaymentType());
		assertEquals("5560000001", in.getBatchOwner().getTaxId());
		assertEquals("B", in.getVoucherSeries());
		assertEquals("SEK", in.getBankAccount().getCurrency());
		assertEquals("1930", in.getBankAccount().getGeneralLedgerBankAccount());
		assertEquals("12345678901", in.getBankAccount().getAccountNo());
		assertEquals("camt053_v02_example.xml", in.getSource());
		assertEquals("2026-10-21", in.getPeriodFrom().toString());

		// 3400 (Ustrd), 20000 (CINV) and 7000 split into 5000 + 2000 (two CINV)
		List<Payment<?>> payments = in.getPayments();
		assertEquals(4, payments.size());
		assertEquals(30400.0, sum(payments), 0.001);

		Payment<?> ustrd = payments.get(0);
		assertTrue(ustrd.isCustomerPayment());
		assertEquals("107208,", ustrd.getDestinationSystemReference());
		assertEquals(CamtToPaymentBatch.REFERENCE_FIELD_INVOICE, ustrd.getDestinationSystemReferenceField());
		assertEquals("Kund Ett AB", ustrd.getBusinessPartner().getName());
		assertEquals("Småstad", ustrd.getBusinessPartner().getAddressOfficial().getCity());
		assertEquals("TX-REF-0003", ustrd.getTransactionReference().getTransactionId());

		Payment<?> cinv = payments.get(1);
		assertEquals("107204", cinv.getDestinationSystemReference());
		assertEquals(20000.0, cinv.getAmount(), 0.001);

		Payment<?> split1 = payments.get(2);
		Payment<?> split2 = payments.get(3);
		assertEquals("107193 5000.00", split1.getDestinationSystemReferenceRaw());
		assertEquals(5000.0, split1.getAmount(), 0.001);
		assertEquals("107194", split2.getDestinationSystemReference());
		assertEquals(2000.0, split2.getAmount(), 0.001);
		assertEquals("TX-REF-0005-2", split2.getTransactionReference().getTransactionId());

		PaymentBatch out = batches.get(1);
		assertEquals(PaymentType.PAYABLE, out.getPaymentType());
		assertEquals(2, out.getPayments().size());
		assertEquals(5066.0, sum(out.getPayments()), 0.001);
		Payment<?> supplier = out.getPayments().get(0);
		assertFalse(supplier.isCustomerPayment());
		assertEquals("8741054725", supplier.getDestinationSystemReference());
		assertEquals(CamtToPaymentBatch.REFERENCE_FIELD_OCR, supplier.getDestinationSystemReferenceField());
		assertEquals("Leverantör Ett AB", supplier.getBusinessPartner().getName());
		assertEquals("1111111", supplier.getAccountNo());
	}

	@Test
	public void testSourceReferenceRegex() throws Exception {

		P27PaymentBatchFactory factory = new P27PaymentBatchFactory(EXAMPLE_DIR);
		factory.getChannelOptions().setSourceReferenceRegex("^(\\d+).*$");
		PaymentBatch in = factory.createPaymentBatchesFromFile("camt053_v02_example.xml").get(0);
		assertEquals("107208", in.getPayments().get(0).getDestinationSystemReference());
		assertEquals("107193", in.getPayments().get(2).getDestinationSystemReference());
	}

	/**
	 * Reads the real bank files and checks that the payments add up to the
	 * statement's totals.
	 */
	@Test
	public void testRealFiles() throws Exception {

		File dir = new File(REAL_FILES_DIR);
		Assume.assumeTrue("No real bank files in " + REAL_FILES_DIR, dir.isDirectory());

		P27PaymentBatchFactory factory = new P27PaymentBatchFactory(dir.getCanonicalPath());
		String[] files = factory.getFilteredFiles();
		assertTrue(files.length > 0);
		for (String file : files) {
			CamtDocument doc = new CamtReader().read(new File(dir, file));
			BigDecimal credits = BigDecimal.ZERO;
			BigDecimal debits = BigDecimal.ZERO;
			for (CamtAccountReport r : doc.getReports()) {
				if (r.getSummary()==null) continue;
				if (r.getSummary().getSumOfCreditEntries()!=null) credits = credits.add(r.getSummary().getSumOfCreditEntries());
				if (r.getSummary().getSumOfDebitEntries()!=null) debits = debits.add(r.getSummary().getSumOfDebitEntries());
			}
			double receivables = 0;
			double payables = 0;
			for (PaymentBatch b : factory.createPaymentBatchesFromFile(file)) {
				assertEquals(file, b.getSource());
				for (Payment<?> p : b.getPayments()) {
					assertNotNull(p.getPaymentDate());
					assertNotNull(p.getTransactionReference().getTransactionId());
				}
				if (b.getPaymentType()==PaymentType.RECEIVABLE)
					receivables += sum(b.getPayments());
				else
					payables += sum(b.getPayments());
			}
			assertEquals(file, credits.doubleValue(), receivables, 0.001);
			assertEquals(file, debits.doubleValue(), payables, 0.001);
		}
	}

	private double sum(List<Payment<?>> payments) {
		BigDecimal sum = BigDecimal.ZERO;
		for (Payment<?> p : payments) {
			sum = sum.add(BigDecimal.valueOf(p.getAmount()));
		}
		return sum.doubleValue();
	}

}
