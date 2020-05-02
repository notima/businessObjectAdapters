package com.svea.businessobjects.paymentgw;

import java.util.List;
import java.util.Map;

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
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.FactoringReservation;

import com.svea.webpay.paymentgw.PaymentGwClient;
import com.svea.webpay.paymentgw.entity.Customer;
import com.svea.webpay.paymentgw.entity.Transaction;

public class SveaPmtGwBusinessObjectFactory extends BasicBusinessObjectFactory <
		PaymentGwClient, 
		Object,		// Invoice 
		com.svea.webpay.paymentgw.entity.Transaction,	// Order
		Object,		// Product
		com.svea.webpay.paymentgw.entity.Customer // BusinessPartner
		>
		
{

	private PaymentGwClient	client = null;

	/**
	 * To initialize this object factory it must be initialized with credentials.
	 * 
	 * @param cardMerchantId
	 * @param cardSecretWord
	 */
	public void init(Integer cardMerchantId, String cardSecretWord) {
		
		client = new PaymentGwClient(cardMerchantId, cardSecretWord);
		
	}
	
	
	@Override
	public String getSystemName() {
		return "SveaPaymentGateway";
	}

	@Override
	public BusinessPartnerList<Customer> listTenants() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTenant(String orgNo, String countryCode) throws NoSuchTenantException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getTenantOrgNo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner<Customer> lookupBusinessPartner(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<Customer>> lookupAllBusinessPartners() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<Customer>> lookupBusinessPartners(int maxCount, boolean customers, boolean suppliers)
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
	public DunningRun<?, ?> lookupDunningRun(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaymentGwClient getClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupNativeInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persistNativeInvoice(Object invoice) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Transaction lookupNativeOrder(String key) throws Exception {

		if (key==null) return null;
		Long transactionId = Long.parseLong(key);
		
		Transaction tr = client.queryByTransactionId(transactionId);
		
		return tr;
	}

	@Override
	public Transaction persistNativeOrder(Transaction order) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Invoice<Object> lookupInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order<Transaction> lookupOrder(String key) throws Exception {

		Transaction tr = lookupNativeOrder(key);
		Order<Transaction> order = SveaPmtGwConverter.convert(tr);
		
		return order;
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
	public List<Product<Object>> lookupProductByName(String name) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PriceList lookupPriceForProduct(String productKey, String currency, Boolean salesPriceList)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ProductCategory> lookupProductCategory(String key) throws Exception {
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
	public FactoringReservation lookupFactoringReservation(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FactoringReservation> lookupFactoringReservationForOrder(String orderKey) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FactoringReservation> lookupFactoringReservationForInvoice(String invoiceKey) throws Exception {
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
	
}
