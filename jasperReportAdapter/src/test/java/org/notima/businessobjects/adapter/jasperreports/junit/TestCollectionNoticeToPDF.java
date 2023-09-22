package org.notima.businessobjects.adapter.jasperreports.junit;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Properties;

import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.jasperreports.JasperBasePdfFormatter;
import org.notima.businessobjects.adapter.jasperreports.JasperInvoiceCollectionFormatter;
import org.notima.businessobjects.adapter.jasperreports.ds.DunningEntryXmlDataSource;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;

public class TestCollectionNoticeToPDF {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {

		try {

			DunningRun<?,?> dr = null;
			Collection<DunningEntry<?,?>> list = null; 
			try {
				list = DunningEntryXmlDataSource.getDunningEntries();
			} catch (Exception e) {
				// Try to find orderlist as resource
				InputStream is = ClassLoader.getSystemResourceAsStream("reminders-example.xml");
				if (is!=null) {
					dr = JAXB.unmarshal(is, DunningRun.class);
					list = dr.getEntries();
				}
			}
			
			Properties props = new Properties();

			DateFormat dfmt = SimpleDateFormat.getDateInstance();
			
			props.put(JasperBasePdfFormatter.JASPER_REPORT_NAME, dfmt.format(Calendar.getInstance().getTime()) + " - The ultimate report");
			
			JasperInvoiceCollectionFormatter formatter = new JasperInvoiceCollectionFormatter();
			formatter.setUsePlusgirot(false);
			
			String outputFileName;
			String path;
			int reminderCount = 1;
			
			for (DunningEntry<?,?> entry : list) {
				outputFileName = entry.getDebtor().getName() + "-" + reminderCount;
				reminderCount++;
				props.put(JasperBasePdfFormatter.JASPER_OUTPUT_FILENAME, outputFileName);
				path = formatter.formatReminder(entry, formatter.getFormats()[0], props);
				System.out.println(path);
			}
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		
	}

}
