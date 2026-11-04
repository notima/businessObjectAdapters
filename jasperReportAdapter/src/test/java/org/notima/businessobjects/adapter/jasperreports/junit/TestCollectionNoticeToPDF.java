package org.notima.businessobjects.adapter.jasperreports.junit;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.junit.Test;
import org.notima.businessobjects.adapter.jasperreports.JasperBasePdfFormatter;
import org.notima.businessobjects.adapter.jasperreports.JasperInvoiceCollectionFormatter;
import org.notima.businessobjects.adapter.jasperreports.JasperInvoiceCollectionFormatter.Template;
import org.notima.generic.ifacebusinessobjects.CollectionNoticeFormatter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Location;

/**
 * Creates a collection notice with each bundled template, in target/collection-notices.
 */
public class TestCollectionNoticeToPDF {

	@Test
	public void testAllTemplates() throws Exception {

		File outDir = new File("target/collection-notices");

		for (Template template : Template.values()) {

			JasperInvoiceCollectionFormatter formatter = new JasperInvoiceCollectionFormatter();

			Properties props = new Properties();
			props.setProperty(CollectionNoticeFormatter.TEMPLATE, template.getKey());
			props.setProperty(JasperBasePdfFormatter.JASPER_OUTPUT_DIR, outDir.getPath());
			props.setProperty(JasperBasePdfFormatter.JASPER_OUTPUT_FILENAME, "inkassokrav-" + template.name());
			props.put(CollectionNoticeFormatter.COLLECTION_FEE, Double.valueOf(180));
			props.put(CollectionNoticeFormatter.INTEREST_RATE, Double.valueOf(0.10));

			String path = formatter.formatCollectionNotice(createEntry(), "pdf", props);
			System.out.println(path);
			File pdf = new File(path);
			assertTrue(path, pdf.length() > 0);
		}

	}

	private static DunningEntry<Object,Object> createEntry() {

		DunningEntry<Object,Object> entry = new DunningEntry<Object,Object>();

		BusinessPartner<Object> creditor = new BusinessPartner<Object>();
		creditor.setName("Exempelföretaget AB");
		creditor.setAddressOfficial(location("Storgatan 1", null, "111 22", "Stockholm", "info@exempel.se", "08-123 45 67"));
		entry.setCreditor(creditor);
		entry.setBgNo("5050-1055");

		BusinessPartner<Object> debtor = new BusinessPartner<Object>();
		debtor.setName("Kalle Kund");
		debtor.setAddressOfficial(location("Lillgatan 2", "Lgh 1101", "222 33", "Lund", null, null));
		entry.setDebtor(debtor);

		entry.addInvoice(invoice("1001", "Medlemsavgift 2026", 1250.50, 90));
		entry.addInvoice(invoice("1002", "Samfällighetsavgift", 3000, 45));
		entry.setLetterNo("1001");
		entry.setOcrNo("100153");
		entry.setReminderFee(60.0);

		DunningRun<Object,Object> run = new DunningRun<Object,Object>();
		run.addDunningEntry(entry);
		return entry;
	}

	private static Invoice<Object> invoice(String no, String description, double openAmt, int daysOverdue) {
		Invoice<Object> inv = new Invoice<Object>();
		inv.setDocumentKey(no);
		inv.setInvoiceKey(no);
		inv.setDescriptionKey(description);
		inv.setOpenAmt(openAmt);
		inv.setCurrency("SEK");
		inv.setDueDate(daysAgo(daysOverdue));
		inv.setInvoiceDate(daysAgo(daysOverdue + 30));
		return inv;
	}

	private static Location location(String address1, String address2, String postal, String city, String email, String phone) {
		Location loc = new Location();
		loc.setAddress1(address1);
		loc.setAddress2(address2);
		loc.setPostal(postal);
		loc.setCity(city);
		loc.setEmail(email);
		loc.setPhone(phone);
		return loc;
	}

	private static Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -days);
		return cal.getTime();
	}

}
