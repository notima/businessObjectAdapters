package org.notima.businessobjects.adapter.fortnox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that can be used as datasource for Jasper Reports or other consumers.
 * 
 * @author Daniel Tamm
 *
 */
public class FortnoxDataSource {

	static Logger logger = LoggerFactory.getLogger(FortnoxDataSource.class);	
	
	private List<String> invoiceNumbersToInclude = new ArrayList<String>();
	private List<String> invoiceNumbersToExclude = new ArrayList<String>();
	
	private List<Invoice<?>> invoiceList;

	private Pattern addrNumPattern = Pattern.compile(".*?\\s(\\d+)$");
	private Map<Integer,List<Invoice<?>>> sortedMap = new TreeMap<Integer,List<Invoice<?>>>();
	
	private FortnoxAdapter fortnoxAdapter;
	
	public FortnoxDataSource(FortnoxAdapter fa) {
		fortnoxAdapter = fa;
	}
	
	
	private void lookupInvoiceAndAddToList(String invoiceId) throws Exception {
		Location c;
		Matcher m;
		int addrNum;
		
		Invoice<?> inv = fortnoxAdapter.lookupInvoice(invoiceId);
		c = inv.getBillLocation()!=null ? inv.getBillLocation() : inv.getBusinessPartner().getAddressOfficial();
		m = addrNumPattern.matcher(c.getAddress1());
		if (m.matches()) {
			addrNum = Integer.parseInt(m.group(1));
		} else {
			addrNum = 0;
		}
		invoiceList = sortedMap.get(addrNum);
		if (invoiceList==null) {
			invoiceList = new ArrayList<Invoice<?>>();
			sortedMap.put(addrNum, invoiceList);
		}
		invoiceList.add(inv);
	}
	
	public Object[] generateUnpostedInvoicesSortedByAddress() throws IOException {

		readInvoiceNumbersToInclude();
		
		List<Invoice<?>> result = new java.util.ArrayList<Invoice<?>>();
		
		try {
			
			Map<Object,Object> infoList = fortnoxAdapter.lookupList(FortnoxAdapter.LIST_UNPOSTED);
			// Put all invoice Ids in a set.
			Set<String> invoiceNumbers = new TreeSet<String>();
			for (Object key : infoList.keySet()) {
				invoiceNumbers.add(key.toString());
			}
			for (String invoiceInclude : invoiceNumbersToInclude) {
				invoiceNumbers.add(invoiceInclude);
			}
			
			for (String invoiceExclude : invoiceNumbersToExclude) {
				invoiceNumbers.remove(invoiceExclude);
			}
			
			for (String key : invoiceNumbers) {
				lookupInvoiceAndAddToList(key);				
			}

			// Now everything is sorted
			for (Integer ii : sortedMap.keySet()) {
				invoiceList = sortedMap.get(ii);
				
				for (Invoice<?> invoice : invoiceList) {
					result.add(invoice);
				}
				
			}
			
			return( result.toArray() );
		
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

	
	private void readInvoiceNumbersToInclude() throws IOException {
		
		String invoiceListFile = System.getenv("FORTNOX_INVOICE_LIST_FILE");
		if (invoiceListFile==null) {
			invoiceListFile = System.getProperty("FORTNOX_INVOICE_LIST_FILE");
			invoiceNumbersToInclude = readNumbersFromFile(invoiceListFile);
		}
		
	}
	
	private void readInvoiceNumbersToExclude() throws IOException {
	
		String invoiceExcludeFile = System.getenv("FORTNOX_INVOICE_EXCLUDE_FILE");
		if (invoiceExcludeFile==null) {
			invoiceExcludeFile = System.getProperty("FORTNOX_INVOICE_EXCLUDE_FILE");
			invoiceNumbersToExclude = readNumbersFromFile(invoiceExcludeFile);
		}
		
		
	}
	
	private List<String> readNumbersFromFile(String fileName) throws IOException {
		List<String> result = new ArrayList<String>();

		if (fileName==null)
			return result;
		File file = new File(fileName);
		if (!file.exists()) {
			logger.warn(fileName + " doesn't exist");
			return result;
		}
		
		
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		do {
			line = br.readLine();
			if (line!=null && line.trim().length()>0) {
				result.add(line.trim());
			}
		} while (line!=null);
		
		br.close();
		
		return result;
		
	}
	
}
