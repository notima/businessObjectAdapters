package org.notima.businessobjects.adapter.jasperreports.junit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.notima.businessobjects.adapter.jasperreports.JasperInvoiceFormatter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.ifacebusinessobjects.InvoiceFormatter;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/**
 * Formats an invoice with each bundled invoice report in Swedish (the default) and in
 * English, and checks the language of the printed texts.
 */
public class TestInvoiceLanguage {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Invoice<?> sampleInvoice(boolean credit, boolean contact) {
		BusinessPartner sender = new BusinessPartner();
		sender.setName("Acme AB");
		sender.setTaxId("556000-0001");
		sender.setVatNo("SE556000000101");
		sender.setApprovedForFTax(true);
		sender.setRemitToAccountType("BG");
		sender.setRemitToAccount("123-4567");
		Location senderLoc = new Location();
		senderLoc.setAddress1("Storgatan 1");
		senderLoc.setPostal("111 11");
		senderLoc.setCity("Stockholm");
		if (contact) {
			senderLoc.setPhone("08-123 45 67");
			senderLoc.setEmail("info@acme.se");
		}
		sender.setAddressOfficial(senderLoc);

		BusinessPartner customer = new BusinessPartner();
		customer.setName("Customer Ltd");
		customer.setIdentityNo("1001");
		Location loc = new Location();
		loc.setAddress1("High Street 2");
		loc.setPostal("SW1A 1AA");
		loc.setCity("London");
		customer.setAddressOfficial(loc);

		Invoice invoice = new Invoice();
		invoice.setSender(sender);
		invoice.setBusinessPartner(customer);
		invoice.setInvoiceKey("1001");
		invoice.setOcr("100107");
		invoice.setCurrency("SEK");
		Calendar cal = Calendar.getInstance();
		cal.set(2026, Calendar.OCTOBER, 16);
		invoice.setInvoiceDate(cal.getTime());
		cal.add(Calendar.DATE, 30);
		invoice.setDueDate(cal.getTime());

		InvoiceLine line = new InvoiceLine();
		line.setKey("A1");
		line.setName("Consulting");
		line.setQtyEntered(2);
		line.setPriceActual(500.0);
		line.setTaxPercent(25);
		line.setLineNet(1000);
		line.setTaxAmount(250);
		List<InvoiceLine> lines = new ArrayList<InvoiceLine>();
		lines.add(line);
		invoice.setLines(lines);
		int sign = credit ? -1 : 1;
		invoice.setNetTotal(sign * 1000);
		invoice.setVatTotal(sign * 250);
		invoice.setGrandTotal(sign * 1250);
		return invoice;
	}

	private static String format(String template, String language, Path dir) throws Exception {
		return format(template, language, dir, false);
	}

	private static String format(String template, String language, Path dir, boolean credit) throws Exception {
		return format(template, language, dir, credit, false);
	}

	private static String format(String template, String language, Path dir, boolean credit, boolean contact) throws Exception {
		Properties props = new Properties();
		props.setProperty(InvoiceFormatter.TEMPLATE_FILE, template);
		props.setProperty(InvoiceFormatter.OUTPUT_DIR, dir.toString());
		props.setProperty(InvoiceFormatter.OUTPUT_FILENAME, template + "_" + (language != null ? language : "default") + (credit ? "_credit" : "") + (contact ? "_contact" : ""));
		if (language != null) props.setProperty(InvoiceFormatter.LANGUAGE, language);
		String path = new JasperInvoiceFormatter().formatInvoice(sampleInvoice(credit, contact), "pdf", props);
		PdfReader reader = new PdfReader(path);
		StringBuilder text = new StringBuilder();
		try {
			PdfTextExtractor extractor = new PdfTextExtractor(reader);
			for (int i = 1; i <= reader.getNumberOfPages(); i++) text.append(extractor.getTextFromPage(i)).append('\n');
		} finally {
			reader.close();
		}
		return text.toString();
	}

	@Test
	public void testLanguages() throws Exception {
		Path dir = Files.createTempDirectory("invoice-language");
		for (String template : JasperInvoiceFormatter.TEMPLATES) {
			String sv = format(template, null, dir);
			// The sample sender has no phone or e-mail: neither label nor "null" is printed
			assertFalse(template + " prints null:\n" + sv, sv.contains("null"));
			assertFalse(template + " prints an empty phone label:\n" + sv, sv.contains("Telefon:"));
			assertTrue(template + " default should be Swedish:\n" + sv, sv.contains("ATT BETALA") && sv.contains("Benämning"));

			String en = format(template, "en_US", dir);
			assertTrue(template + " should be English:\n" + en, en.contains("AMOUNT DUE") && en.contains("Description"));
			assertFalse(template + " should have no Swedish invoice labels:\n" + en, en.contains("ATT BETALA") || en.contains("Benämning"));
			assertTrue(template + " footer:\n" + en, en.contains("Approved for F-tax"));

			// A language without translation gets the default (Swedish) texts
			String de = format(template, "de_DE", dir);
			assertTrue(template + " untranslated language:\n" + de, de.contains("ATT BETALA"));
		}
		// Phone and e-mail are printed when set
		for (String template : JasperInvoiceFormatter.TEMPLATES) {
			String text = format(template, null, dir, false, true);
			assertTrue(template + " contact:\n" + text, text.contains("Telefon:") && text.contains("08-123 45 67")
					&& text.contains("E-post:") && text.contains("info@acme.se"));
		}

		// InvoiceBasic shows the credit title next to its title
		assertTrue(format("InvoiceBasic", "en_US", dir, true).contains("CREDIT"));
		assertTrue(format("InvoiceBasic", null, dir, true).contains("KREDITERING"));
		assertTrue(format("InvoicePlain", "en_US", dir, true).contains("CREDIT NOTE"));
		for (File f : dir.toFile().listFiles()) f.delete();
		dir.toFile().delete();
	}
}
