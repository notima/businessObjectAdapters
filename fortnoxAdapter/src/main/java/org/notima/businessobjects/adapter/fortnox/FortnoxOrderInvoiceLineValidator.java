package org.notima.businessobjects.adapter.fortnox;

import org.notima.generic.businessobjects.TaxSubjectIdentifier;

import java.util.List;

import org.notima.api.fortnox.FortnoxConstants;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.businessobjects.exception.TaxRatesNotAvailableException;
import org.notima.generic.ifacebusinessobjects.OrderInvoice;
import org.notima.generic.ifacebusinessobjects.OrderInvoiceLine;
import org.notima.generic.ifacebusinessobjects.OrderInvoiceLineValidator;
import org.notima.generic.ifacebusinessobjects.TaxRateProvider;
import org.notima.util.LocalDateUtils;

public class FortnoxOrderInvoiceLineValidator implements OrderInvoiceLineValidator {

	private TaxRateProvider 	taxRateProvider;
	
	private OrderInvoice 		orderInvoice;
	private OrderInvoiceLine	theLine;
	private TaxSubjectIdentifier tsi;
	private List<Tax>		    validTaxRates;
	private String				validationMessage;
	private String				taxDomicile;
	private boolean				adjustToClosestTaxRate = false;
	
	/**
	 * 
	 * @param documentSender		The owner of the document. Normally the sender of a customer invoice (i.e.).
	 * @param trp					Tax rate provider.  
	 */
	public FortnoxOrderInvoiceLineValidator(TaxSubjectIdentifier documentSender, TaxRateProvider trp) {
		
		tsi = documentSender;
		taxRateProvider = trp;
		
	}

	@Override
	public void setOrderInvoice(OrderInvoice oi) {
		orderInvoice = oi;
		taxDomicile = orderInvoice.getTaxDomicile();
		if (orderInvoice!=null) {
			try {
				
				if (taxDomicile!=null)
					validTaxRates = taxRateProvider.getValidTaxRates(tsi, taxDomicile, LocalDateUtils.asLocalDate(oi.getDocumentDate()));
				else
					validTaxRates = taxRateProvider.getValidTaxRates(tsi, LocalDateUtils.asLocalDate(oi.getDocumentDate()));
				
			} catch (NoSuchTenantException | TaxRatesNotAvailableException nste) {
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
		
		if (!checkValidTaxRate())
			return false;
		
		return true;
		
	}
	
	private boolean checkValidTaxRate() {

		// By pass rounding
		if ("rounding".equals(theLine.getKey())) {
			theLine.setTaxPercent(0);
			theLine.setTaxKey("0");
			return true;
		}
		// Calculate tax rate
		double lineTaxRate = theLine.getTaxPercent();
		Tax closestTaxRate = findClosestTaxRate(lineTaxRate);
		
		if (lineTaxRate == closestTaxRate.getRate())
			return true;
		else {
			validationMessage = "Actual tax rate: " + lineTaxRate + ". Closest tax rate: " + closestTaxRate.getRate();
			if (adjustToClosestTaxRate) {
				adjustLineToTaxRate(closestTaxRate.getRate());
				return true;
			}
		}
		
		return false;
		
	}
	
	private void adjustLineToTaxRate(double newRate) {

		double totalPriceIncVAT;
		if (theLine.isPricesIncludeVAT()) {
			totalPriceIncVAT = theLine.getPriceActual();
			theLine.setTaxPercent(newRate);
		} else {
			totalPriceIncVAT = theLine.getPriceActual() * (1 + (theLine.getTaxPercent()/100.0));
			theLine.setPriceActual(totalPriceIncVAT / (1 + (newRate / 100.0)));
			theLine.setTaxPercent(newRate);
		}
		theLine.calculateLineTotalIncTax(FortnoxConstants.DEFAULT_ROUNDING_PRECISION);

		validationMessage += ". Adjusted to tax rate: " + newRate;
		
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
	
	public String getTaxDomicile() {
		return taxDomicile;
	}

	public TaxRateProvider getTaxRateProvider() {
		return taxRateProvider;
	}

	public void setTaxRateProvider(TaxRateProvider taxRateProvider) {
		this.taxRateProvider = taxRateProvider;
	}

	public boolean isAdjustToClosestTaxRate() {
		return adjustToClosestTaxRate;
	}

	public void setAdjustToClosestTaxRate(boolean adjustToClosestTaxRate) {
		this.adjustToClosestTaxRate = adjustToClosestTaxRate;
	}

}
