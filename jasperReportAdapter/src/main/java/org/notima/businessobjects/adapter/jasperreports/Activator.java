package org.notima.businessobjects.adapter.jasperreports;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.businessobjects.adapter.tools.InvoiceFormatter;
import org.notima.businessobjects.adapter.tools.OrderListFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(OrderListFormatter.class),
				@ProvideService(InvoiceFormatter.class),
		}
)
public class Activator extends BaseActivator {

	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	public void doStart() {

		JasperOrderListFormatter formatter = new JasperOrderListFormatter();
		log.info("Created JasperOrderListFormatter");
		register(OrderListFormatter.class, formatter);
		
		JasperInvoiceFormatter invoiceFormatter = new JasperInvoiceFormatter();
		log.info("Create JasperInvoiceFormatter");
		register(InvoiceFormatter.class, invoiceFormatter);
		
	}
	
	
}
