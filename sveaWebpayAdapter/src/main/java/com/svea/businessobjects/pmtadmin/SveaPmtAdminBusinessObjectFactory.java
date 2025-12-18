package com.svea.businessobjects.pmtadmin;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.notima.api.webpay.pmtapi.PmtApiClientCollection;
import org.notima.api.webpay.pmtapi.PmtApiCredential;
import org.notima.api.webpay.pmtapi.exception.NoSuchOrderException;
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
		PmtApiClientCollection, 
		org.notima.api.webpay.pmtapi.entity.Invoice, 
		org.notima.api.webpay.pmtapi.CheckoutOrder,
		Object,
		Object,
		Object
		> {

	private PmtApiClientCollection clients;
	
	public SveaPmtAdminBusinessObjectFactory() {
		clients = new PmtApiClientCollection();
	}

	/**
	 * Initializes the business object factory
	 * 
	 * @param serverName
	 * @param orgNo				The orgno this merchant id belongs to.
	 * @param merchantId
	 * @param secretWord
	 */
	public void addCredential(String serverName, String orgNo, String merchantId, String secretWord) {
		
		PmtApiCredential credential = new PmtApiCredential();
		credential.setServer(serverName);
		credential.setOrgNo(orgNo);
		credential.setMerchantId(merchantId);
		credential.setSecret(secretWord);
		clients.addPmtApiClient(credential);

	}

	@Override
	public Order<org.notima.api.webpay.pmtapi.CheckoutOrder> lookupOrder(String key) throws Exception {
		org.notima.api.webpay.pmtapi.CheckoutOrder o = null;
		try {
			o = lookupNativeOrder(key);
		} catch (NoSuchOrderException nsoe) {
			
		}
		if (o==null) return null;
		Order<org.notima.api.webpay.pmtapi.CheckoutOrder> result = SveaPmtAdminConverter.convert(o);
		result.setNativeOrder(o);
		// Add merchantId as attribute
		result.addAttribute("merchantId", o.getMerchantId());
		return result;
	}

	@Override
	public PmtApiClientCollection getClient() {
		return clients;
	}

	@Override
	public org.notima.api.webpay.pmtapi.CheckoutOrder lookupNativeOrder(String key)
			throws Exception {
		
		return clients.getCheckoutOrder(Long.parseLong(key));
		
	}

	@Override
	public BusinessPartner<?> lookupBusinessPartner(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<?>> lookupAllBusinessPartners()
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public DunningRun lookupDunningRun(String key, Date dueDateUntil) throws Exception {
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
	public org.notima.api.webpay.pmtapi.CheckoutOrder persistNativeOrder(
			org.notima.api.webpay.pmtapi.CheckoutOrder order) throws Exception {
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
	public Map<Object, Object> lookupList(String listName, boolean customer) throws Exception {
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
	public BusinessPartner<Object> lookupThisCompanyInformation() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<?>> lookupBusinessPartners(int maxCount,
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
