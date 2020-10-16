package org.notima.businessobjects.adapter.tools;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Reference;

public class FormatterFactoryImpl implements FormatterFactory {

	@Reference
	ConfigurationAdmin configAdmin;
	
	private Map<String, OrderListFormatter> services = new TreeMap<String, OrderListFormatter>();
	private Map<String, InvoiceReminderFormatter> invoiceReminderServices = new TreeMap<String, InvoiceReminderFormatter>();

	private BundleContext ctx;
	
	public void setBundleContext(BundleContext c) {
		ctx = c;
	}
	
	/**
	 * Resets the services by re-reading the service references for WebpayProvisioningService
	 * 
	 * @throws InvalidSyntaxException
	 */
	public void resetServices() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<OrderListFormatter>> references = ctx.getServiceReferences(OrderListFormatter.class, null);		

		services.clear();
		
		if (references!=null) {
			OrderListFormatter srv;
			for (ServiceReference<OrderListFormatter> sr : references) {
				srv = ctx.getService(sr);
				String[] formats = srv.getFormats();
				if (formats!=null) {
					for (String s : formats) {
						services.put(s, srv);
					}
				}
			}
		}

		Collection<ServiceReference<InvoiceReminderFormatter>> irefs = ctx.getServiceReferences(InvoiceReminderFormatter.class, null);		
		
		invoiceReminderServices.clear();
		
		if (irefs!=null) {
			InvoiceReminderFormatter srv;
			for (ServiceReference<InvoiceReminderFormatter> sr : irefs) {
				srv = ctx.getService(sr);
				String[] formats = srv.getFormats();
				if (formats!=null) {
					for (String s : formats) {
						invoiceReminderServices.put(s, srv);
					}
				}
			}
		}
		
	}
	
	@Override
	public OrderListFormatter getFormatter(String format) {

		try {
			resetServices();
			return services.get(format);
		} catch (Exception ee) {
			ee.printStackTrace();
		}
		
		return null;
	}

	
	/**
	 * Returns an invoice formatter for given format.
	 * 
	 * @param format		The format.
	 * @return	An invoice reminder formatter (if any).
	 */
	public InvoiceReminderFormatter getInvoiceReminderFormatter(String format) {
		
		try {
			resetServices();
			return invoiceReminderServices.get(format);
		} catch (Exception ee) {
			ee.printStackTrace();
		}
		
		return null;
		
		
	}
	
}
