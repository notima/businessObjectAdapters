package org.notima.businessobjects.adapter.fortnox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.businessobjects.exception.TaxRatesNotAvailableException;

/**
 * Hard coded class since tax rates doesn't yet seem to be available from the Fortnox API.
 * 
 */
public class FortnoxTaxRateFetcher {
	
	private double[] taxRates_SE = new double[] {
		25,
		12,
		6,
		0
	};
	
	private double[] taxRates_FI = new double[] {
		24,
		14,
		10,
		0
	};

	private Map<String, double[]> taxRateMapByCountry = new TreeMap<String, double[]>();
	
	private final String DEFAULT_COUNTRY_CODE = "SE";
	
	private FortnoxAdapter bof;
	private FortnoxClient3 fc3;
	private TaxSubjectIdentifier tsi;
	private List<Tax> lastValidRates;
	
	public FortnoxTaxRateFetcher(FortnoxAdapter bof, TaxSubjectIdentifier tsi) throws NoSuchTenantException {

		initTaxRates();
		
		this.bof = bof;
		this.tsi = tsi;
		this.bof.setTenant(this.tsi.getTaxId(), this.tsi.getCountryCode());
		fc3 = bof.getClient();
		lastValidRates = new ArrayList<Tax>();
	}

	private void initTaxRates() {
		
		taxRateMapByCountry.put("SE", taxRates_SE);
		taxRateMapByCountry.put("FI", taxRates_FI);
		
	}
	
	public List<Tax> getValidTaxes(LocalDate taxDate, String targetCountry) throws TaxRatesNotAvailableException {

		// TODO: Extend this to actually query Fortnox.
		// Currently we make a simple method.
		lastValidRates.clear();
		
		Set<String> countries = null;
		
		if (targetCountry==null) {
			countries = taxRateMapByCountry.keySet();
		} else {
			if (taxRateMapByCountry.get(targetCountry)!=null) {
				countries = new TreeSet<String>();
				countries.add(targetCountry);
			} else {
				throw new TaxRatesNotAvailableException(targetCountry);
			}
		}

		for (String tc : countries) {
		
			double[] taxRates = taxRateMapByCountry.get(tc);
			
			Tax tax;
			for (double rate : taxRates) {
				tax = new Tax();
				tax.setCountryCode(tc);
				tax.setRate(rate);
				tax.setKey(Long.toString((Math.round(rate))));
				lastValidRates.add(tax);
			}
			
		}
		
		return lastValidRates;
		
	}
	
	public List<Tax> getValidTaxes(LocalDate taxDate) throws TaxRatesNotAvailableException {

		return getValidTaxes(taxDate, null);
		
	}
	
}
