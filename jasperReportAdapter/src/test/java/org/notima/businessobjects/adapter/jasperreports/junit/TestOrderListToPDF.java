package org.notima.businessobjects.adapter.jasperreports.junit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.jasperreports.JasperOrderListFormatter;
import org.notima.businessobjects.adapter.jasperreports.ds.OrderListXmlDataSource;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderList;

import com.ibm.icu.util.Calendar;

public class TestOrderListToPDF {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {

		try {
			
			Collection<Order<?>> list = OrderListXmlDataSource.getOrderList();
			
			Properties props = new Properties();

			DateFormat dfmt = SimpleDateFormat.getDateInstance();
			
			props.put(JasperOrderListFormatter.JASPER_COMPANY_NAME, "Testing Company AB");
			props.put(JasperOrderListFormatter.JASPER_REPORT_NAME, dfmt.format(Calendar.getInstance().getTime()) + " - The ultimate report");
			props.put(JasperOrderListFormatter.JASPER_TAX_ID, "555555-5555");
			
			OrderList ol = new OrderList();
			List<Order<?>> orderList = new ArrayList<Order<?>>();
			orderList.addAll(list);
			ol.setOrderList(orderList);
			JasperOrderListFormatter formatter = new JasperOrderListFormatter();
			String path = formatter.formatOrderList(ol, formatter.getFormats()[0], props);

			System.out.println(path);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		
	}

}
