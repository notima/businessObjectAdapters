package org.notima.businessobjects.adapter.json;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.notima.businessobjects.adapter.json.impl.TenantList;
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

public class JsonAdapter<C,I,O,P,B,T> extends BasicBusinessObjectFactory<C, I, O, P, B, T> {

	public static final String	SYSTEM_NAME= "Json";
	
	private JsonPropertyFile	properties;

	private TenantList			tenantList;
	
	public JsonAdapter(JsonPropertyFile p) {
		properties = p;
		
		try {
			initTenantList();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void initTenantList() throws IOException {
		if (properties.getTenantFile()!=null) {
			tenantList = TenantList.createFromFile(new File(properties.getTenantFile()));
			refreshTenantMap();
		}
	}
	
	@Override
	public String getSystemName() {
		return SYSTEM_NAME;
	}

	@SuppressWarnings("unchecked")
	@Override
	public BusinessPartnerList<T> listTenants() {
		return tenantList.getTenants();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public BusinessPartner<T> addTenant(String orgNo, String countryCode, String name, Properties props) {
		BusinessPartner<T> result = super.addTenant(orgNo, countryCode, name, props);
		tenantList.getTenants().getBusinessPartner().add(result);
		try {
			tenantList.saveToFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public BusinessPartner<B> lookupBusinessPartner(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<?>> lookupAllBusinessPartners() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<?>> lookupBusinessPartners(int maxCount, boolean customers, boolean suppliers)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner<T> lookupThisCompanyInformation() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DunningRun<?, ?> lookupDunningRun(String key, Date dueDateUntil) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public C getClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public I lookupNativeInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public I persistNativeInvoice(I invoice) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public O lookupNativeOrder(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public O persistNativeOrder(O order) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Invoice<I> lookupInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order<O> lookupOrder(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<P> lookupProduct(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<P> lookupProductByEan(String ean) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product<P>> lookupProductByName(String name) throws Exception {
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
	public Product<P> lookupRoundingProduct() throws Exception {
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
