package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Location;

/**
 * Class that can be used as datasource for Jasper Reports or other consumers.
 * 
 * @author Daniel Tamm
 *
 */
public class FortnoxDataSource {

	public static Object[] generateUnpostedInvoicesSortedByAddress(FortnoxAdapter fa) {
		
		Pattern addrNumPattern = Pattern.compile(".*?\\s(\\d+)$");
		
		List<Invoice<?>> result = new java.util.ArrayList<Invoice<?>>();
		
		try {

			String invoiceId;
			
			Invoice<?> inv;
			
			Map<Object,Object> infoList = fa.lookupList(FortnoxAdapter.LIST_UNPOSTED);
			Location c;
			Matcher m;
			int addrNum;
			Map<Integer,List<Invoice<?>>> sortedMap = new TreeMap<Integer,List<Invoice<?>>>();
			List<Invoice<?>> invoiceList;
			for (Object key : infoList.keySet()) {
				invoiceId = key.toString();				
				inv = fa.lookupInvoice(invoiceId);
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
	
}
