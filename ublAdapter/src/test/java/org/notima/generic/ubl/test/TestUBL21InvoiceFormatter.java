package org.notima.generic.ubl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.xml.bind.JAXB;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.notima.businessobjects.adapter.tools.InvoiceFormatter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.ubl.factory.UBL21Converter;
import org.notima.generic.ubl.factory.UBL21InvoiceFormatter;

import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

class TestUBL21InvoiceFormatter {

	@TempDir
	Path dir;

	private Invoice<?> loadSampleInvoice() throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("sample-bo-invoice.xml")) {
			assertNotNull(in, "sample-bo-invoice.xml not found");
			return JAXB.unmarshal(in, Invoice.class);
		}
	}

	private Properties props(String fileName) {
		Properties props = new Properties();
		props.setProperty(InvoiceFormatter.OUTPUT_DIR, dir.toString());
		props.setProperty(InvoiceFormatter.OUTPUT_FILENAME, fileName);
		return props;
	}

	@Test
	void invoiceWithBankgiro() throws Exception {
		Invoice<?> invoice = loadSampleInvoice();
		BusinessPartner<?> sender = invoice.getSender();
		sender.setRemitToAccount("123-4567");
		sender.setRemitToAccountType("BG");

		String path = new UBL21InvoiceFormatter().formatInvoice(invoice, UBL21InvoiceFormatter.FORMAT_PEPPOL, props("inv"));

		File file = new File(path);
		assertEquals(new File(dir.toFile(), "inv.xml").getAbsolutePath(), path);
		assertTrue(file.exists());
		InvoiceType ubl = UBL21Converter.readFromString(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
		assertNotNull(ubl);
		assertEquals(1, ubl.getPaymentMeans().size());
		// Peppol SE-R-008: numeric bankgiro
		assertEquals("1234567", ubl.getPaymentMeans().get(0).getPayeeFinancialAccount().getIDValue());
		assertEquals("SE:BANKGIRO", ubl.getPaymentMeans().get(0).getPayeeFinancialAccount().getFinancialInstitutionBranch().getIDValue());
	}

	@Test
	void invoiceWithIbanWithoutBic() throws Exception {
		Invoice<?> invoice = loadSampleInvoice();
		invoice.getSender().setRemitToIBAN("SE45 5000 0000 0583 9825 7466");

		String path = new UBL21InvoiceFormatter().formatInvoice(invoice, UBL21InvoiceFormatter.FORMAT_PEPPOL, props("iban"));

		InvoiceType ubl = UBL21Converter.readFromString(new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8));
		assertEquals("SE4550000000058398257466", ubl.getPaymentMeans().get(0).getPayeeFinancialAccount().getIDValue());
		assertEquals(null, ubl.getPaymentMeans().get(0).getPayeeFinancialAccount().getFinancialInstitutionBranch());
	}

	@Test
	void negativeInvoiceIsCreditNote() throws Exception {
		Invoice<?> invoice = loadSampleInvoice();
		for (InvoiceLine line : invoice.getLines()) {
			line.setQtyEntered(-line.getQtyEntered());
		}
		invoice.calculateGrandTotal();
		double grandTotal = invoice.getGrandTotal();
		assertTrue(grandTotal < 0);

		String path = new UBL21InvoiceFormatter().formatInvoice(invoice, UBL21InvoiceFormatter.FORMAT_PEPPOL, props("credit"));

		String xml = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
		assertTrue(xml.contains("<CreditNote "), "Expected a CreditNote document");
		// Amounts in a credit note are positive
		assertTrue(xml.contains("<cbc:PayableAmount currencyID=\"SEK\">" + (-grandTotal) + "</cbc:PayableAmount>"), xml);
		assertTrue(!xml.contains(">-"), "Negative value in credit note: " + xml);
		// The invoice is restored
		assertEquals(grandTotal, invoice.getGrandTotal());
		assertTrue(invoice.getLines().get(1).getQtyEntered() < 0);
	}

}
