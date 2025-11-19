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
	private Map<String, InvoiceFormatter> invoiceFormatters = new TreeMap<String, InvoiceFormatter>();
	private Map<String, InvoiceReminderFormatter> invoiceReminderServices = new TreeMap<String, InvoiceReminderFormatter>();
	// The class name is the key
	private Map<String, ReportFormatter<?>> basicReportFormatters = new TreeMap<String, ReportFormatter<?>>();

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
		
		Collection<ServiceReference<InvoiceFormatter>> ifrefs = ctx.getServiceReferences(InvoiceFormatter.class, null);		
		
		invoiceFormatters.clear();
		
		if (irefs!=null) {
			InvoiceFormatter srv;
			for (ServiceReference<InvoiceFormatter> sr : ifrefs) {
				srv = ctx.getService(sr);
				String[] formats = srv.getFormats();
				if (formats!=null) {
					for (String s : formats) {
						invoiceFormatters.put(s, srv);
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
	 * Returns a formatter for given class and format.
	 * 
	 * @param format
	 * @return	A formatter for given class. Null if none is found.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public ReportFormatter<?> getReportFormatter(Class<?> clazz, String format) {

		basicReportFormatters.clear();
		
		try {
			Collection<ServiceReference<ReportFormatter>> irefs = ctx.getServiceReferences(ReportFormatter.class, null);
			
			if (irefs!=null) {
				ReportFormatter<?> srv;
				for (ServiceReference<ReportFormatter> sr : irefs) {
					srv = ctx.getService(sr);
					String className = srv.getClazz().getClass().getName();
					basicReportFormatters.put(className, srv);
				}
			}
			
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Try to find report formatter
		ReportFormatter<?> result = basicReportFormatters.get(clazz.getName());
		
		boolean supportsFormat = false;
		
		if (result!=null) {
			// Check format
			String[] formats = result.getFormats();
			for (String f : formats) {
				if (f.equalsIgnoreCase(format)) {
					supportsFormat = true;
					break;
				}
			}
		}
		
		if (supportsFormat) return result;
		
		return null;
		
	}
	
	public InvoiceFormatter getInvoiceFormatter(String format) {

		try {
			resetServices();
			return invoiceFormatters.get(format);
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
