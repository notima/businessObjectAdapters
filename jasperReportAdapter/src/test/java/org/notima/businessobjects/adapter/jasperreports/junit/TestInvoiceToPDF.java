package org.notima.businessobjects.adapter.jasperreports.junit;

import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Properties;

import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.jasperreports.JasperBasePdfFormatter;
import org.notima.businessobjects.adapter.jasperreports.JasperInvoiceFormatter;
import org.notima.businessobjects.adapter.jasperreports.ds.InvoiceListXmlDataSource;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;

public class TestInvoiceToPDF {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {

		try {

			InvoiceList dr = null;
			Collection<Invoice<?>> list = null; 
			try {
				list = InvoiceListXmlDataSource.getInvoiceList();
			} catch (Exception e) {
				// Try to find orderlist as resource
				URL url = ClassLoader.getSystemResource("invoices-example.xml");
				InputStream is = ClassLoader.getSystemResourceAsStream("invoices-example.xml");
				if (is!=null) {
					dr = JAXB.unmarshal(is, InvoiceList.class);
					list = dr.getInvoiceList();
					System.setProperty(InvoiceListXmlDataSource.INVOICELIST_XML_FILE, url.getFile());
				}
			}
			
			Properties props = new Properties();

			DateFormat dfmt = SimpleDateFormat.getDateInstance();
			
			props.put(JasperBasePdfFormatter.JASPER_REPORT_NAME, dfmt.format(Calendar.getInstance().getTime()) + " - The ultimate report");
			
			JasperInvoiceFormatter formatter = new JasperInvoiceFormatter();
			
			String path = formatter.formatInvoice(list.iterator().next(), formatter.getFormats()[0], props);

			System.out.println(path);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		
	}

}
