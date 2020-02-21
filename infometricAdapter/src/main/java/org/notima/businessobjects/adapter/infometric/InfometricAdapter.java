package org.notima.businessobjects.adapter.infometric;

/**
 * 
 * Copyright 2020 Notima System Integration AB (Sweden)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Daniel Tamm
 *
 */

import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.ifacebusinessobjects.OrderInvoice;

/**
 * Converts an Infometric billing file to a list of Business objects OrderInvoice records
 * 
 * @author Daniel Tamm
 *
 */
public class InfometricAdapter {

	public SimpleDateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd");
	public NumberFormat nfmt;
	public DecimalFormatSymbols nsyms;
	
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
	public List<OrderInvoice> splitBillingFile(
								String productKey, 
								double price, 
								String invoiceLineText, 
								String fileContent) throws IOException, ParseException        {
		
		nsyms = new DecimalFormatSymbols();
		nsyms.setDecimalSeparator(',');
		nfmt = new DecimalFormat("##.##", nsyms);
		
		List<OrderInvoice> result = new ArrayList<OrderInvoice>();
		
		CSVFormat fmt = CSVFormat.EXCEL.withDelimiter(';');
		
		Iterable<CSVRecord> rows = fmt.parse(new StringReader(fileContent));
		
		Invoice<?> invoice = null;
		BusinessPartner<?> bp;
		InvoiceLine il;
		
		List<InvoiceLine> ilines;
		
		for (CSVRecord r : rows) {
			
			if (r.size()==0) continue;
			invoice = new Invoice<Object>();
			invoice.setCurrency("SEK");
			bp = new BusinessPartner<Object>();
			bp.setIdentityNo(r.get(0));  // APT No, to be remapped
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
		
		return result;
		
	}
	
}
