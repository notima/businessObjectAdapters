package org.notima.businessobjects.adapter.fortnox;

import org.notima.generic.businessobjects.TaxSubjectIdentifier;

import java.util.List;

import org.notima.generic.businessobjects.Tax;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.OrderInvoice;
import org.notima.generic.ifacebusinessobjects.OrderInvoiceLine;
import org.notima.generic.ifacebusinessobjects.OrderInvoiceLineValidator;
import org.notima.generic.ifacebusinessobjects.TaxRateProvider;
import org.notima.util.LocalDateUtils;

public class FortnoxOrderInvoiceLineValidator implements OrderInvoiceLineValidator {

	private TaxRateProvider taxRateProvider;
	
	private OrderInvoice 		orderInvoice;
	private OrderInvoiceLine	theLine;
	private TaxSubjectIdentifier tsi;
	private List<Tax>		    validTaxRates;
	private String				validationMessage;
	
	public FortnoxOrderInvoiceLineValidator(TaxSubjectIdentifier documentSender, TaxRateProvider trp) {
		
		tsi = documentSender;
		taxRateProvider = trp;
		
	}

	@Override
	public void setOrderInvoice(OrderInvoice oi) {
		orderInvoice = oi;
		if (orderInvoice!=null) {
			try {
				validTaxRates = taxRateProvider.getValidTaxRates(tsi, LocalDateUtils.asLocalDate(oi.getDocumentDate()));
			} catch (NoSuchTenantException nste) {
				nste.printStackTrace();
			}
		}
	}
	
	@Override
	public void setLineToValidate(OrderInvoiceLine line) {
		theLine = line;
		validationMessage = null;
	}

	
	@Override
	public boolean isLineValid() {
		// Calculate tax rate
		double lineTaxRate = theLine.getTaxPercent();
		Tax closestTaxRate = findClosestTaxRate(lineTaxRate);
		
		if (lineTaxRate == closestTaxRate.getRate())
			return true;
		else
			validationMessage = "Actual tax rate: " + lineTaxRate + ". Closest tax rate: " + closestTaxRate.getRate();
		
		return false;
	}

	@Override
	public String getValidationMessage() {
		return validationMessage;
	}

	private Tax findClosestTaxRate(double actualTaxRate) {
		
		Tax closestTaxRate = validTaxRates.get(0);
		double minDifference = Math.abs(actualTaxRate - closestTaxRate.getRate());
		
		for (Tax t : validTaxRates) {
			double difference = Math.abs(actualTaxRate - t.getRate());
			if (difference < minDifference) {
				minDifference = difference;
				closestTaxRate = t;
			}
		}
		
		return closestTaxRate;
	}
	
	

}
