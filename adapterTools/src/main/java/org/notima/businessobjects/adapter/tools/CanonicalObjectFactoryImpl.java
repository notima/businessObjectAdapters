package org.notima.businessobjects.adapter.tools;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.AccountingVoucherConverter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectConverter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.notima.generic.ifacebusinessobjects.TaxRateProvider;
import org.notima.generic.ifacebusinessobjects.TimeRecordServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CanonicalObjectFactoryImpl implements CanonicalObjectFactory {

	private Logger log = LoggerFactory.getLogger(CanonicalObjectFactoryImpl.class);	

	@SuppressWarnings("rawtypes")
	private Map<String, BusinessObjectFactory> services = new TreeMap<String, BusinessObjectFactory>();
	@SuppressWarnings("rawtypes")
	private Map<String, BusinessObjectConverter> converters = new TreeMap<String, BusinessObjectConverter>();

	private Map<String, AccountingVoucherConverter<?>> accountingVoucherConverters = new TreeMap<String, AccountingVoucherConverter<?>>();
	
	private Map<String, PaymentFactory> paymentFactories = new TreeMap<String, PaymentFactory>();

	private Map<String, PaymentBatchFactory> paymentBatchFactories = new TreeMap<String, PaymentBatchFactory>();
	
	private Map<String, TimeRecordServiceFactory> timeRecordServiceFactories = new TreeMap<String, TimeRecordServiceFactory>();
	
	private Map<String, PaymentBatchProcessor> paymentBatchProcessors = new TreeMap<String, PaymentBatchProcessor>();
	
	private Map<String, TaxRateProvider> taxRateProviders = new TreeMap<String, TaxRateProvider>();
	
	private Map<String, PaymentBatchChannelFactory> paymentBatchChannelFactories = new TreeMap<String, PaymentBatchChannelFactory>();
	
	private BundleContext ctx;

	public void setBundleContext(BundleContext c) {
		ctx = c;
	}
	
	/**
	 * Resets the services by re-reading the service references for WebpayProvisioningService
	 * 
	 * @throws InvalidSyntaxException
	 */
	@SuppressWarnings("rawtypes")
	public void resetServices() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<BusinessObjectFactory>> references = ctx.getServiceReferences(BusinessObjectFactory.class, null);		

		services.clear();
		
		if (references!=null) {
			BusinessObjectFactory srv;
			for (ServiceReference<BusinessObjectFactory> sr : references) {
				srv = ctx.getService(sr);
				services.put(srv.getSystemName(), srv);
			}
		}

	}

	/**
	 * Resets the services by re-reading the service references for WebpayProvisioningService
	 * 
	 * @throws InvalidSyntaxException
	 */
	@SuppressWarnings("rawtypes")
	public void resetConverters() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<BusinessObjectConverter>> references = ctx.getServiceReferences(BusinessObjectConverter.class, null);		

		services.clear();
		
		if (references!=null) {
			BusinessObjectConverter srv;
			for (ServiceReference<BusinessObjectConverter> sr : references) {
				srv = ctx.getService(sr);
				converters.put(srv.getSystemName(), srv);
			}
		}

	}
	
	/**
	 * Resets the accounting voucher converters
	 * 
	 * @throws InvalidSyntaxException
	 */
	@SuppressWarnings("rawtypes")
	public void resetAccountingVoucherConverters() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<AccountingVoucherConverter>> references = ctx.getServiceReferences(AccountingVoucherConverter.class, null);		

		accountingVoucherConverters.clear();
		
		if (references!=null) {
			AccountingVoucherConverter srv;
			for (ServiceReference<AccountingVoucherConverter> sr : references) {
				srv = ctx.getService(sr);
				accountingVoucherConverters.put(srv.getSystemName(), srv);
			}
		}

	}
	
	
	/**
	 * Resets the services by re-reading the service references for PaymentFactories
	 * 
	 * @throws InvalidSyntaxException
	 */
	public void resetPaymentFactories() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<PaymentFactory>> references = ctx.getServiceReferences(PaymentFactory.class, null);		

		paymentFactories.clear();
		
		if (references!=null) {
			PaymentFactory srv;
			for (ServiceReference<PaymentFactory> sr : references) {
				srv = ctx.getService(sr);
				paymentFactories.put(srv.getSystemName(), srv);
			}
		}

	}
	
	/**
	 * Resets the services by re-reading the service references for PaymentFactories
	 * 
	 * @throws InvalidSyntaxException
	 */
	public void resetPaymentBatchFactories() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<PaymentBatchFactory>> references = ctx.getServiceReferences(PaymentBatchFactory.class, null);		

		paymentBatchFactories.clear();
		
		if (references!=null) {
			PaymentBatchFactory srv;
			for (ServiceReference<PaymentBatchFactory> sr : references) {
				srv = ctx.getService(sr);
				paymentBatchFactories.put(srv.getSystemName(), srv);
			}
		}

	}
	
	
	/**
	 * Resets the services by re-reading the service references for TimeRecordServices
	 * 
	 * @throws InvalidSyntaxException
	 */
	public void resetTimeRecordServices() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<TimeRecordServiceFactory>> references = ctx.getServiceReferences(TimeRecordServiceFactory.class, null);		

		timeRecordServiceFactories.clear();
		
		if (references!=null) {
			TimeRecordServiceFactory srv;
			for (ServiceReference<TimeRecordServiceFactory> sr : references) {
				srv = ctx.getService(sr);
				timeRecordServiceFactories.put(srv.getSystemName(), srv);
			}
		}

	}
	
	
	/**
	 * Resets the services by re-reading the service references for PaymentFactories
	 * 
	 * @throws InvalidSyntaxException
	 */
	public void resetPaymentBatchChannelFactories() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<PaymentBatchChannelFactory>> references = ctx.getServiceReferences(PaymentBatchChannelFactory.class, null);		

		paymentFactories.clear();
		
		if (references!=null) {
			PaymentBatchChannelFactory srv;
			for (ServiceReference<PaymentBatchChannelFactory> sr : references) {
				srv = ctx.getService(sr);
				paymentBatchChannelFactories.put(srv.getSystemName(), srv);
			}
		}

	}
	

	/**
	 * Resets the services by re-reading the service references for PaymentBatchProcessors
	 * 
	 * @throws InvalidSyntaxException
	 */
	public void resetPaymentBatchProcessors() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<PaymentBatchProcessor>> references = ctx.getServiceReferences(PaymentBatchProcessor.class, null);		

		paymentBatchProcessors.clear();
		
		if (references!=null) {
			PaymentBatchProcessor srv;
			for (ServiceReference<PaymentBatchProcessor> sr : references) {
				srv = ctx.getService(sr);
				paymentBatchProcessors.put(srv.getSystemName(), srv);
			}
		}

	}
	
	public void resetTaxRateProviders() throws InvalidSyntaxException {

		if (ctx==null)
			ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
		
		Collection<ServiceReference<TaxRateProvider>> references = ctx.getServiceReferences(TaxRateProvider.class, null);		

		taxRateProviders.clear();
		
		if (references!=null) {
			TaxRateProvider srv;
			for (ServiceReference<TaxRateProvider> sr : references) {
				srv = ctx.getService(sr);
				taxRateProviders.put(srv.getSystemName(), srv);
			}
		}
		
		
	}
	
	public Collection<PaymentBatchProcessor> listPaymentBatchProcessors() {
		try {
			resetPaymentBatchProcessors();
			return paymentBatchProcessors.values();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
	}
	
	public Collection<PaymentFactory> listPaymentFactories() {
		try {
			resetPaymentFactories();
			return paymentFactories.values();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
	}
	
	public Collection<TimeRecordServiceFactory> listTimeRecordServices() {
		
		try {
			resetTimeRecordServices();
			return timeRecordServiceFactories.values();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	
	
	/**
	 * Looks up an adapter with given name.
	 * 
	 * @param adapterName			The name of the adapter to find.		
	 * @return		Null if not found. The adapter if found.
	 */
	@SuppressWarnings("rawtypes")
	public BusinessObjectFactory lookupAdapter(String adapterName) {

		try {
			resetServices();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
		
		BusinessObjectFactory bf = null;
		
		if (adapterName==null) {
			return null;
		} else {
			bf = services.get(adapterName);
		}
		if (bf==null)
			log.warn("Adapter {} not found.", adapterName);
		
		return bf;
		
	}
	
	@Override
	public TaxRateProvider lookupTaxRateProvider(String adapterName) {

		try {
			resetTaxRateProviders();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
		
		TaxRateProvider trp = null;
		
		if (adapterName==null) {
			return null;
		} else {
			trp = taxRateProviders.get(adapterName);
		}
		if (trp==null)
			log.warn("Tax rate provider: {} not found.", adapterName);
		
		return trp;
		
	}
	
	
	/**
	 * Looks up an adapter with given name.
	 * 
	 * @param adapterName			The name of the adapter to find.		
	 * @return		Null if not found. The adapter if found.
	 */
	@SuppressWarnings("rawtypes")
	public BusinessObjectConverter lookupConverter(String adapterName) {

		try {
			resetConverters();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
		
		BusinessObjectConverter cv = null;
		
		if (adapterName==null) {
			return null;
		} else {
			cv = converters.get(adapterName);
		}
		if (cv==null)
			log.warn("Converter {} not found.", adapterName);
		
		return cv;
		
	}

	@Override
	public AccountingVoucherConverter<?> lookupAccountingVoucherConverter(String adapterName) {
		
		try {
			resetAccountingVoucherConverters();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
		
		AccountingVoucherConverter<?> cv = null;
		
		if (adapterName==null) {
			return null;
		} else {
			cv = accountingVoucherConverters.get(adapterName);
		}
		if (cv==null)
			log.warn("Accounting converter {} not found.", adapterName);
		
		return cv;
		
	}
	
	public PaymentFactory lookupPaymentFactory(String systemName) {
		
		try {
			resetPaymentFactories();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
		
		PaymentFactory pp = null;
		
		if (systemName==null) {
			return null;
		} else {
			pp = paymentFactories.get(systemName);
		}
		if (pp==null)
			log.warn("Payment Factory {} not found.", systemName);
		
		return pp;
		
	}
	
	public PaymentBatchFactory lookupPaymentBatchFactory(String systemName) {
		
		try {
			resetPaymentBatchFactories();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
		
		PaymentBatchFactory pp = null;
		
		if (systemName==null) {
			return null;
		} else {
			pp = paymentBatchFactories.get(systemName);
		}
		if (pp==null)
			log.warn("Payment Batch Factory {} not found.", systemName);
		
		return pp;
		
	}
	
	
	public PaymentBatchProcessor lookupPaymentBatchProcessor(String systemName) {
		
		try {
			resetPaymentBatchProcessors();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
		
		PaymentBatchProcessor pp = null;
		
		if (systemName==null) {
			return null;
		} else {
			pp = paymentBatchProcessors.get(systemName);
		}
		if (pp==null)
			log.warn("Payment Batch Processor {} not found.", systemName);
		
		return pp;
		
		
	}
	
	
	
	@Override
	@SuppressWarnings("rawtypes")	
	public Invoice<?> lookupCustomerInvoice(String adapterName, String orgNo, String countryCode, String invoiceNo) throws NoSuchTenantException {

		BusinessObjectFactory bf = lookupAdapter(adapterName);
		
		if (bf==null) return null;

		// Set tenant
		bf.setTenant(orgNo, countryCode);
		Invoice<?> invoice = null;
		try {
			invoice = bf.lookupInvoice(invoiceNo);
		} catch (Exception e) {
			log.error("Error looking up customer invoice " + invoiceNo + " for " + orgNo + " in adapter " + adapterName);
		}
		
		return invoice;
	}
	
	
	/**
	 * Converts a canonical invoice to a given adapter format.
	 * 
	 * @param adapterName		The adapter name
	 * @param source			The source invoice
	 * @param props				Properties sent to the converter.
	 * @return		A string representation of the adapter format.
	 * @throws Exception 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object convertToNativeInvoice(String adapterName, Invoice<?> source, Properties props) throws Exception {
		
		BusinessObjectConverter cv = lookupConverter(adapterName);
		
		if (cv==null) {
			log.warn("Converter for {} not found");
			return null;
		}
		
		Object result = cv.toNativeInvoice(source);
		
		return result;
		
	}

	@Override
	public Collection<TaxRateProvider> listTaxRateProviders() {
		try {
			resetTaxRateProviders();
			return taxRateProviders.values();
		} catch (Exception e) {
			// This should not happen.
			e.printStackTrace();
			return null;
		}
	}

	
	@Override
	public PaymentBatchChannelFactory lookupFirstPaymentBatchChannelFactory() {
		try {
			resetPaymentBatchChannelFactories();
			if (!paymentBatchChannelFactories.isEmpty()) {
				return paymentBatchChannelFactories.get(paymentBatchChannelFactories.keySet().iterator().next());
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	
	
}
