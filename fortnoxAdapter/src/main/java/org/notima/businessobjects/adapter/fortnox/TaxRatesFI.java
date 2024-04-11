package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.Tax;

public class TaxRatesFI {

	private final String countryCode = "FI";
	
	private List<Tax> taxRates;
	
	public TaxRatesFI() {

		taxRates = new ArrayList<Tax>();
		Tax t = new Tax("24", countryCode, 24, "MP4_KUND", "OUTVAT_MP4_KUND", "INVAT");
		t.setDefaultRevenueAccount(new AccountElement("3151"));
		t.setTaxDebtAccount(new AccountElement("2690"));
		taxRates.add(t);
		
		t = new Tax("14", countryCode, 14, "MP5_KUND", "OUTVAT_MP5_KUND", "INVAT");		
		t.setDefaultRevenueAccount(new AccountElement("3152"));
		t.setTaxDebtAccount(new AccountElement("2691"));
		taxRates.add(t);
		
		t = new Tax("10", countryCode, 10, "MP6_KUND", "OUTVAT_MP6_KUND", "INVAT");		
		t.setDefaultRevenueAccount(new AccountElement("3153"));
		t.setTaxDebtAccount(new AccountElement("2992"));
		taxRates.add(t);
		
		t = new Tax("0", countryCode, 0, null, null, null);		
		t.setDefaultRevenueAccount(new AccountElement("3055"));
		t.setTaxDebtAccount(new AccountElement("3055"));
		taxRates.add(t);

	}
	
	public List<Tax> getTaxRates() {

		return taxRates;
		
	}
	
}
