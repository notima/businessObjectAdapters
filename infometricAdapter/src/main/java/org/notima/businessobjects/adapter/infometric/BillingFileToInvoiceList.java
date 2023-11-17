package org.notima.businessobjects.adapter.infometric;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.InvoiceList;

public class BillingFileToInvoiceList {

	private NumberFormat nfmt;
	private DecimalFormatSymbols nsyms;
	private DateFormat dfmt = new SimpleDateFormat("yyMMdd");

	private Date today = Calendar.getInstance().getTime();
	
	private InfometricAdapter adapter;
	
	private InfometricTenant tenant;
	
	public BillingFileToInvoiceList(InfometricAdapter adapter, InfometricTenant tenant) {
		this.adapter = adapter;
		this.tenant = tenant;
	}

	public InvoiceList readAllFiles(double price) throws Exception {
		
		String[] fileNames = getTxtFiles();
		InvoiceList totalList = new InvoiceList();
		totalList.setInvoiceList(new ArrayList<Invoice<?>>());
		InvoiceList ilist;
		for (String fileName : fileNames) {
			ilist = billingFileToInvoiceList(getAbsolutePath() + File.separator + fileName, price);
			 totalList.getInvoiceList().addAll(ilist.getInvoiceList());
		}

		return totalList;
	}
	
	
	public InvoiceList billingFileToInvoiceList(String file, double price) throws IOException, ParseException {
		
		byte[] fileBytes = Files.readAllBytes(Paths.get(file));
		String contents = new String(fileBytes, "UTF-8");
		
		InvoiceList list = splitBillingFile(tenant.getProductKey(), price, tenant.getInvoiceLineText(), contents);
		
		return list;
		
	}
	
	
	
	/**
	 * Converts a CSV (semicolon separated) file to a list of OrderInvoice
	 * 
	 * @param productKey			The product key to use for the billing line
	 * @param price					The price per unit
	 * @param invoiceLineText		The text to describe the product. Dates are appended to this line.
	 * @param fileContent			The actual file to be parsed.
	 * @return						A list.
	 * @throws IOException 			If something goes wrong.
	 * @throws ParseException       If the numbers can't be parsed.
	 */
	public InvoiceList splitBillingFile(
								String productKey, 
								double price, 
								String invoiceLineText, 
								String fileContent) throws IOException, ParseException        {
		
		nsyms = new DecimalFormatSymbols();
		nsyms.setDecimalSeparator(',');
		nfmt = new DecimalFormat("##.##", nsyms);
		
		InvoiceList list = new InvoiceList();
		List<Invoice<?>> result = new ArrayList<Invoice<?>>();
		list.setInvoiceList(result);
		
		CSVFormat fmt = CSVFormat.EXCEL.withDelimiter(';');
		
		Iterable<CSVRecord> rows = fmt.parse(new StringReader(fileContent));
		
		Invoice<?> invoice = null;
		BusinessPartner<?> bp;
		InvoiceLine il;
		
		List<InvoiceLine> ilines;
		
		String aptNo;
		
		for (CSVRecord r : rows) {
			if (r.size()==0) continue;
			aptNo = r.get(0);
			invoice = new Invoice<Object>();
			invoice.setCurrency("SEK");
			invoice.setOrderKey(getOrderKey(aptNo));
			bp = new BusinessPartner<Object>();
			bp.setIdentityNo(aptNo);  // APT No, to be remapped
			invoice.setBusinessPartner(bp);
			ilines = new ArrayList<InvoiceLine>();
			il = new InvoiceLine();
			il.setProductKey(productKey);		// Product key
			il.setUOM(r.get(10));				// kWh
			il.setQtyEntered(nfmt.parse(r.get(6)).doubleValue());
			il.setPriceActual(price!=0.0 ? price : nfmt.parse(r.get(9)).doubleValue());
			il.setName(invoiceLineText + " " + r.get(1) + " - " + r.get(2));
			ilines.add(il);
			invoice.setLines(ilines);
			result.add(invoice);
			
		}
		
		return list;
		
	}

	private String getOrderKey(String prefix) {
		return prefix + "-" + dfmt.format(today);
	}
	
	private String getAbsolutePath() {
		if (!tenant.getTenantDirectory().startsWith(File.separator)) {
			return adapter.baseDirectory + File.separator + tenant.getTenantDirectory();
		} else {
			return tenant.getTenantDirectory();
		}
	}
	
	private String[] getTxtFiles() {
		String[] files = new File(getAbsolutePath()).list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith("txt"))
					return true;
				return false;
			}});
		return files;
	}

	
}
