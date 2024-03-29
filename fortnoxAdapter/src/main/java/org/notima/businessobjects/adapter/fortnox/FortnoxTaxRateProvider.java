package org.notima.businessobjects.adapter.fortnox;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.notima.generic.businessobjects.Tax;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.businessobjects.exception.TaxRatesNotAvailableException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.TaxRateProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FortnoxTaxRateProvider implements BundleActivator, TaxRateProvider {

	private Logger log = LoggerFactory.getLogger(FortnoxTaxRateProvider.class);	
	
	@SuppressWarnings("rawtypes")
    private ServiceReference<BusinessObjectFactory> serviceReference;
	@SuppressWarnings("rawtypes")
	private BusinessObjectFactory bofService;
	
    @SuppressWarnings("rawtypes")
	@Override
    public void start(BundleContext bundleContext) throws Exception {
        // Create a service filter based on the "System" property
        String filter = "(SystemName=Fortnox)";

    	Collection<ServiceReference<BusinessObjectFactory>> services = bundleContext.getServiceReferences(BusinessObjectFactory.class, filter);
    	ServiceReference<BusinessObjectFactory> ref = services != null && services.size()>0 ? services.iterator().next() : null;
    	if (ref!=null) {
    		bofService = bundleContext.getService(ref);
    	}

        if (bofService == null) {
        	log.warn("No Fortnox Business Object Adapter found.");
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        // Unget the service when stopping the bundle
        if (serviceReference != null) {
            bundleContext.ungetService(serviceReference);
        }
    }	
	
	
	@Override
	public String getSystemName() {
		return FortnoxAdapter.SYSTEMNAME;
	}

	@Override
	public String getTaxDomicile() {
		// Sweden
		return "SE";
	}
	
	
	@Override
	public List<Tax> getValidTaxRates(TaxSubjectIdentifier tsi, LocalDate taxDate) throws NoSuchTenantException, TaxRatesNotAvailableException {

		FortnoxTaxRateFetcher ftrf = new FortnoxTaxRateFetcher((FortnoxAdapter)bofService, tsi);
		
		List<Tax> validTaxes = ftrf.getValidTaxes(taxDate);
		
		return validTaxes;
	}

	@Override
	public List<Tax> getValidTaxRates(TaxSubjectIdentifier tsi, String tradingCountry, LocalDate taxDate) throws NoSuchTenantException, TaxRatesNotAvailableException {
		
		FortnoxTaxRateFetcher ftrf = new FortnoxTaxRateFetcher((FortnoxAdapter)bofService, tsi);
		
		List<Tax> validTaxes = ftrf.getValidTaxes(taxDate);
		
		return validTaxes;
		
	}


}
