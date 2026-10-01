package org.notima.generic.ubl.factory;

import java.io.File;
import java.util.Properties;

import org.notima.businessobjects.adapter.tools.InvoiceFormatter;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ESuccess;
import com.helger.ubl21.UBL21Writer;

import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

/**
 * Formats invoices as e-invoices (UBL 2.1, Peppol BIS Billing 3.0).
 *
 * Invoices with a negative grand total are written as credit notes, with positive amounts.
 * The payment means are taken from the payment information (remit to account and IBAN)
 * of the invoice's sender.
 */
@SuppressWarnings("rawtypes")
public class UBL21InvoiceFormatter implements InvoiceFormatter {

	public static final String FORMAT_PEPPOL = "peppol";

	private Logger log = LoggerFactory.getLogger(UBL21InvoiceFormatter.class);

	@Override
	public String formatInvoice(Invoice<?> invoice, String format, Properties props) throws Exception {

		if (invoice==null) {
			throw new Exception("Invoice can't be null");
		}
		if (invoice.getSender()==null) {
			throw new Exception("Invoice " + invoice.getInvoiceKey() + " has no sender (seller). An e-invoice needs one.");
		}

		File outFile = getOutputFile(invoice, props);
		ESuccess result;

		if (invoice.getGrandTotal() < 0) {
			// A credit note has positive amounts. Negate while converting, then restore.
			CreditNoteType creditNote;
			negateAmounts(invoice);
			try {
				creditNote = UBL21Converter.convertToCreditNote((Invoice)invoice);
			} finally {
				negateAmounts(invoice);
			}
			result = UBL21Writer.creditNote().write(creditNote, outFile);
		} else {
			InvoiceType ublInvoice = UBL21Converter.convert((Invoice)invoice);
			String paymentRef = (invoice.getOcr()!=null && invoice.getOcr().trim().length()>0)
					? invoice.getOcr().trim() : invoice.getInvoiceKey();
			if (!UBL21Converter.addPaymentMeans(ublInvoice, invoice.getSender(), paymentRef)) {
				log.warn("No payment information for the sender of invoice {}. The e-invoice has no payment means.", invoice.getInvoiceKey());
			}
			result = UBL21Writer.invoice().write(ublInvoice, outFile);
		}

		if (result.isFailure()) {
			throw new Exception("Failed to write e-invoice " + invoice.getInvoiceKey() + " to " + outFile.getAbsolutePath());
		}

		return outFile.getAbsolutePath();
	}

	/**
	 * Negates the amounts used when converting to a credit note.
	 */
	private void negateAmounts(Invoice<?> invoice) {
		invoice.setGrandTotal(-invoice.getGrandTotal());
		if (invoice.getLines()==null) return;
		for (InvoiceLine line : invoice.getLines()) {
			line.setQtyEntered(-line.getQtyEntered());
			line.setLineNet(-line.getLineNet());
			line.setTaxAmount(-line.getTaxAmount());
		}
	}

	private File getOutputFile(Invoice<?> invoice, Properties props) {

		String outputDir = props!=null ? props.getProperty(OUTPUT_DIR) : null;
		String fileName = props!=null ? props.getProperty(OUTPUT_FILENAME) : null;
		if (fileName==null || fileName.trim().isEmpty()) {
			fileName = invoice.getInvoiceKey();
		}

		File dir = new File(outputDir!=null && !outputDir.trim().isEmpty() ? outputDir.trim() : ".");
		dir.mkdirs();
		return new File(dir, fileName + ".xml");
	}

	@Override
	public String[] getFormats() {
		return new String[] { FORMAT_PEPPOL };
	}

}
