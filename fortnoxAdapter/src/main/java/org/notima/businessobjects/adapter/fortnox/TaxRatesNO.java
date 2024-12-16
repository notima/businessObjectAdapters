package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.Tax;

public class TaxRatesNO {

	private final String countryCode = "NO";
	
	private List<Tax> taxRates;
	
	public TaxRatesNO() {

		taxRates = new ArrayList<Tax>();
		Tax t = new Tax("25", countryCode, 25, "MP4_KUND", "OUTVAT_MP4_KUND", "INVAT");
		t.setDefaultRevenueAccount(new AccountElement("3151"));
		t.setTaxDebtAccount(new AccountElement("2690"));
		taxRates.add(t);
		
		t = new Tax("15", countryCode, 15, "MP5_KUND", "OUTVAT_MP5_KUND", "INVAT");		
		t.setDefaultRevenueAccount(new AccountElement("3152"));
		t.setTaxDebtAccount(new AccountElement("2691"));
		taxRates.add(t);
		
		t = new Tax("12", countryCode, 12, "MP6_KUND", "OUTVAT_MP6_KUND", "INVAT");		
		t.setDefaultRevenueAccount(new AccountElement("3153"));
		t.setTaxDebtAccount(new AccountElement("2992"));
		taxRates.add(t);
		
		t = new Tax("0", countryCode, 0, "E", null, null);		
		t.setDefaultRevenueAccount(new AccountElement("3105"));
		t.setTaxDebtAccount(new AccountElement("3105"));
		taxRates.add(t);

	}
	
	public List<Tax> getTaxRates() {

		return taxRates;
		
	}
	
}
