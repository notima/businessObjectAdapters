package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;

import org.junit.Test;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxException;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;

public class TestFortnoxAdapterMethods extends FortnoxAdapterTestBase {

	// TODO: Make below dynamic depending on the client information
	public static final String BP_NO = "1";
	public static final String INVOICE_NO = "1";
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLookupBusinessPartner() {
		
		try {
			// TODO: Make business partner lookup dynamic
			BusinessPartner<org.notima.api.fortnox.entities3.Customer> bp = factory.lookupBusinessPartner(BP_NO);
			if (bp!=null) {
				log.info("Looked up business partner: {}", bp.getName());
			} else {
				log.warn("Didn't find business partner with no: {}", BP_NO);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLookupInvoice() {
		try {
			// TODO: Make invoice lookup dynamic
			Invoice<org.notima.api.fortnox.entities3.Invoice> invoice = factory.lookupInvoice(INVOICE_NO);
			BusinessPartner<?> bp = invoice.getBusinessPartner();
			log.info(
				"InvoiceNo: " + invoice.getDocumentKey() + " Date: " + dfmt.format(invoice.getDocumentDate()) + 
				" Businesspartner: " + bp.getName() + " [" + bp.getTaxId() + "] " +  
				" Grand total: " + invoice.getGrandTotal());
			
			// Print invoice lines
			InvoiceLine[] lines = invoice.getLines().toArray(new InvoiceLine[0]);
			InvoiceLine line;
			for (int i=0; i<lines.length; i++) {
				line = lines[i];
				log.info(line.getKey() + " " + line.getName() + " " + line.getQtyEntered() + " " + line.getPriceActual() + " " + line.getTaxPercent());
			}
			
		} catch (FortnoxException fe) {
			if (fe.getErrorInformation()!=null && fe.getErrorInformation().getCode().equals(FortnoxClient3.ERROR_CANT_FIND_INVOICE)) {
				log.info(fe.toString());
			} else {
				log.warn(fe.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testLookupDunningRun() {
		try {
			
			DunningRun<?,?> dr = factory.lookupDunningRun("excludeNegativeOpenAmount");
			if (dr!=null && dr.getEntries()!=null) {
				log.info("Retrieved {} entries in a dunning run.", dr.getEntries().size());
			} else {
				log.warn("No dunning entries found. It might be OK.");
			}
			StringWriter stw = new StringWriter();
			Writer wr = new PrintWriter(stw);
			JAXBContext ctx = JAXBContext.newInstance(org.notima.api.fortnox.entities3.Invoice.class, org.notima.generic.businessobjects.DunningRun.class);
			ctx.createMarshaller().marshal(dr, wr);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
