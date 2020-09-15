package com.svea.businessobjects.pmtadmin;

import java.util.List;
import java.util.Map;

import org.notima.api.webpay.pmtapi.PmtApiClientRF;
import org.notima.generic.businessobjects.BasicBusinessObjectFactory;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.PaymentTerm;
import org.notima.generic.businessobjects.PriceList;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductCategory;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.ifacebusinessobjects.FactoringReservation;

/**
 * Business object factory used to convert orders from 
 * 
 * 
 * @author Daniel Tamm
 *
 */
public class SveaPmtAdminBusinessObjectFactory extends BasicBusinessObjectFactory<
		PmtApiClientRF, 
		org.notima.api.webpay.pmtapi.entity.Invoice, 
		org.notima.api.webpay.pmtapi.entity.Order,
		Object,
		Object
		> {

	private PmtApiClientRF client;
	
	public SveaPmtAdminBusinessObjectFactory() {
		client = new PmtApiClientRF();
	}

	/**
	 * Initializes the business object factory
	 * 
	 * @param serverName
	 * @param merchantId
	 * @param secretWord
	 */
	public void init(String serverName, String merchantId, String secretWord) {
		client.init(serverName, merchantId, secretWord);
	}
	
	/**
	 * Initializes the business object factory using a configfile
	 */
	public void loadConfig(String configFile) throws Exception {
		client.loadConfig(configFile);
	}

	@Override
	public Order<org.notima.api.webpay.pmtapi.entity.Order> lookupOrder(String key) throws Exception {
		org.notima.api.webpay.pmtapi.entity.Order o = lookupNativeOrder(key);
		if (o==null) return null;
		Order<org.notima.api.webpay.pmtapi.entity.Order> result = SveaPmtAdminConverter.convert(o);
		result.setNativeOrder(o);
		return result;
	}

	@Override
	public PmtApiClientRF getClient() {
		return client;
	}

	@Override
	public org.notima.api.webpay.pmtapi.entity.Order lookupNativeOrder(String key)
			throws Exception {
		
		return client.getOrder(Long.parseLong(key));
		
	}

	@Override
	public BusinessPartner<Object> lookupBusinessPartner(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<Object>> lookupAllBusinessPartners()
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DunningRun lookupDunningRun(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.notima.api.webpay.pmtapi.entity.Invoice lookupNativeInvoice(
			String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.notima.api.webpay.pmtapi.entity.Invoice persistNativeInvoice(
			org.notima.api.webpay.pmtapi.entity.Invoice invoice)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.notima.api.webpay.pmtapi.entity.Order persistNativeOrder(
			org.notima.api.webpay.pmtapi.entity.Order order) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Invoice<org.notima.api.webpay.pmtapi.entity.Invoice> lookupInvoice(
			String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupProduct(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupProductByEan(String ean) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product<Object>> lookupProductByName(String name)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Object, Object> lookupList(String listName) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupRoundingProduct() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tax lookupTax(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaymentTerm lookupPaymentTerm(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FactoringReservation lookupFactoringReservation(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FactoringReservation> lookupFactoringReservationForOrder(
			String orderKey) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FactoringReservation> lookupFactoringReservationForInvoice(
			String invoiceKey) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persist(Object o) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PriceList lookupPriceForProduct(String productKey, String currency,
			Boolean salesPriceList) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ProductCategory> lookupProductCategory(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner<?> lookupThisCompanyInformation() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<Object>> lookupBusinessPartners(int maxCount,
			boolean customers, boolean suppliers) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartnerList<Object> listTenants() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSystemName() {
		return "Webpay-PmtAdmin";
	}


}
