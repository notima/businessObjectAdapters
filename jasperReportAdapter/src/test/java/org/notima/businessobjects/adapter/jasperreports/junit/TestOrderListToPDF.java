package org.notima.businessobjects.adapter.jasperreports.junit;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.jasperreports.JasperOrderListFormatter;
import org.notima.businessobjects.adapter.jasperreports.ds.OrderListXmlDataSource;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderList;

import java.util.Calendar;

public class TestOrderListToPDF {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {

		try {

			OrderList ol = null;
			Collection<Order<?>> list = null; 
			try {
				list = OrderListXmlDataSource.getOrderList();
				ol = new OrderList();
				List<Order<?>> orderList = new ArrayList<Order<?>>();
				orderList.addAll(list);
				ol.setOrderList(orderList);
			} catch (Exception e) {
				// Try to find orderlist as resource
				InputStream is = ClassLoader.getSystemResourceAsStream("orders-example.xml");
				if (is!=null) {
					ol = JAXB.unmarshal(is, OrderList.class);
				}
			}
			
			Properties props = new Properties();

			DateFormat dfmt = SimpleDateFormat.getDateInstance();
			
			props.put(JasperOrderListFormatter.JASPER_COMPANY_NAME, "Testing Company AB");
			props.put(JasperOrderListFormatter.JASPER_REPORT_NAME, dfmt.format(Calendar.getInstance().getTime()) + " - The ultimate report");
			props.put(JasperOrderListFormatter.JASPER_TAX_ID, "555555-5555");
			
			JasperOrderListFormatter formatter = new JasperOrderListFormatter();
			String path = formatter.formatOrderList(ol, formatter.getFormats()[0], props);

			System.out.println(path);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		
	}

}
