package org.notima.businessobjects.adapter.fortnox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;

public class FortnoxTaxRateFetcher {

	private double[] taxRates = new double[] {
		25,
		12,
		6,
		0
	};
	
	private final String DEFAULT_COUNTRY_CODE = "SE";
	
	
	private FortnoxAdapter bof;
	private FortnoxClient3 fc3;
	private TaxSubjectIdentifier tsi;
	private List<Tax> lastValidRates;
	
	public FortnoxTaxRateFetcher(FortnoxAdapter bof, TaxSubjectIdentifier tsi) throws NoSuchTenantException {
		
		this.bof = bof;
		this.tsi = tsi;
		this.bof.setTenant(this.tsi.getTaxId(), this.tsi.getCountryCode());
		fc3 = bof.getClient();
		lastValidRates = new ArrayList<Tax>();
	}
	
	public List<Tax> getValidTaxes(LocalDate taxDate) {

		// TODO: Extend this to actually query Fortnox.
		// Currently we make a simple method.
		lastValidRates.clear();
		Tax tax;
		for (double rate : taxRates) {
			tax = new Tax();
			tax.setCountryCode(DEFAULT_COUNTRY_CODE);
			tax.setRate(rate);
			tax.setKey(Long.toString((Math.round(rate))));
			lastValidRates.add(tax);
		}
		
		return lastValidRates;
		
	}
	
}
