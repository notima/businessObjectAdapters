package org.notima.businessobjects.adapter.jasperreports.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXB;

import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderList;

public class OrderListXmlDataSource {

	public static final String ORDERLIST_XML_FILE = "ORDERLIST_XML_FILE";
	
	public static Collection<Order<?>> getOrderList() throws Exception {
		
		List<Order<?>> orderList = new ArrayList<Order<?>>();

		String xmlFile = System.getenv(ORDERLIST_XML_FILE);
		if (xmlFile==null) {
			xmlFile = System.getProperty(ORDERLIST_XML_FILE);
		}
		File inFile = null;
		if (xmlFile==null) {
			throw new Exception("Environment variable " + ORDERLIST_XML_FILE + " is not set.");
		} else {
			inFile = new File(xmlFile);
			if (!inFile.canRead()) {
				throw new Exception(xmlFile + " can't be read.");
			}
		}
		
		OrderList result = JAXB.unmarshal(inFile, OrderList.class);
		
		orderList.addAll(result.getOrderList());
			
		return orderList;
		
	}
	
	
}
