package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;
import org.notima.generic.businessobjects.Tax;

public class TaxRatesFI {

	private final String countryCode = "FI";
	
	private List<Tax> taxRates;
	
	public TaxRatesFI() {

		taxRates = new ArrayList<Tax>();
		taxRates.add(new Tax("24", countryCode, 24, "MP4_KUND", "OUTVAT_MP4_KUND", "INVAT"));
		
	}
	
	public List<Tax> getTaxRates() {

		return taxRates;
		
	}
	
}
