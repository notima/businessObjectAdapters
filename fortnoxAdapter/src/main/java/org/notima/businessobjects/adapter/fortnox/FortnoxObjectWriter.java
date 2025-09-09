package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.OrderInvoiceWriterOptions;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.OrderInvoice;

public class FortnoxObjectWriter {

	protected FortnoxAdapter	adapter;
	protected FortnoxClient3 fortnoxClient;
	
	// Create an e-mail / bp number mapping
	protected Map<String, String> custMap = new TreeMap<String,String>();
	protected Map<String, BusinessPartner<?>> custMapById = new TreeMap<String, BusinessPartner<?>>();
	protected Map<TaxSubjectIdentifier, BusinessPartner<?>> custMapByTaxId = new TreeMap<TaxSubjectIdentifier, BusinessPartner<?>>();
	
	protected OrderInvoiceWriterOptions	options;
	
	protected void refreshCustomerMaps() throws Exception {
		
		Location loc;
		List<BusinessPartner<?>> bpList = getAllActiveBusinessPartners();
		
		for (BusinessPartner<?> bp : bpList) {
			if (bp.getAddressOfficial().getAddress1()==null) {
				System.out.println("Problem with " + bp.getName() + " : " + bp.getIdentityNo());
			}
			// Use delivery address for mapping if it exists
			loc = bp.getAddressShipping()!=null ? bp.getAddressShipping() : bp.getAddressOfficial();
			custMap.put(loc.getAddress1().trim().toUpperCase(), bp.getIdentityNo());
			custMapById.put(bp.getIdentityNo(), bp);
			custMapByTaxId.put(new TaxSubjectIdentifier(bp), bp);
			FortnoxAdapter.logger.info("Adding " + bp.getName() + " on " + loc.getAddress1() + " => " + bp.getIdentityNo());
		}
		
	}
	
	
	protected Customer getFortnoxCustomer(OrderInvoice invoice) throws Exception {
		if (invoice==null) return null;
		BusinessPartner<?> bp = invoice.getBusinessPartner();
		BusinessPartner<?> fbp;
		if (bp==null) return null;
		if (bp.hasTaxId()) {
			fbp = custMapByTaxId.get(new TaxSubjectIdentifier(FortnoxUtil.convertTaxIdToFortnoxFormat(bp.getTaxId())));
			if (fbp==null) {
				fbp = custMapByTaxId.get(new TaxSubjectIdentifier(bp.getTaxId()));
			}
		} else {
			fbp = custMapById.get(bp.getIdentityNo());
		}
		return lookupFortnoxCustomerFromBusinessPartner(fbp);
	}

	protected Customer lookupFortnoxCustomerFromBusinessPartner(BusinessPartner<?> fbp) throws Exception {
		Customer fortnoxCustomer = null;
		if (fbp==null) return null;
		if (fbp.getNativeBusinessPartner()!=null) {
			if (fbp.getNativeBusinessPartner() instanceof Customer) {
				fortnoxCustomer = (Customer)fbp.getNativeBusinessPartner();
			} else {
				fortnoxCustomer = fortnoxClient.getCustomerByCustNo(fbp.getIdentityNo());
			}
		} else {
			fortnoxCustomer = fortnoxClient.getCustomerByCustNo(fbp.getIdentityNo());
		}

		return fortnoxCustomer;
	}
	
	protected List<BusinessPartner<?>> getAllActiveBusinessPartners() throws Exception {
		
		// Get all customers
		List<BusinessPartner<Customer>> bpList = adapter.lookupAllActiveCustomers();
		FortnoxAdapter.logger.info(bpList.size() + " existing customers");
		List<BusinessPartner<?>> result = new ArrayList<BusinessPartner<?>>();
		for (BusinessPartner<?> bp : bpList) {
			bp = adapter.lookupBusinessPartner(bp.getIdentityNo());
			if (bp.getNativeBusinessPartner()!=null && bp.getNativeBusinessPartner() instanceof Customer &&  
					(((Customer)bp.getNativeBusinessPartner()).getCurrency()==null || ((Customer)bp.getNativeBusinessPartner()).getCurrency().trim().length()==0)) {
				throw new Exception("No default currency set on customer " + bp.getIdentityNo());
			}
			result.add(bp);
		}
		
		return result;
	}
	
	
}
