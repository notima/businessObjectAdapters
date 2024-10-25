package org.notima.businessobjects.adapter.infometric;

import java.io.File;

/**
 * 
 * Copyright 2020 Notima System Integration AB (Sweden)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Daniel Tamm
 *
 */

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.notima.generic.businessobjects.BasicBusinessObjectFactory;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.PaymentTerm;
import org.notima.generic.businessobjects.PriceList;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductCategory;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.ifacebusinessobjects.FactoringReservation;

/**
 * Converts an Infometric billing file to a list of Business objects OrderInvoice records
 * 
 * @author Daniel Tamm
 *
 */
public class InfometricAdapter extends BasicBusinessObjectFactory<
			Object,
			Object,
			Object,
			Object,
			Object,
			InfometricTenant> {

	public static final String SYSTEM = "Infometric";
	
	public static final String PROP_TENANTDIRECTORY = "tenantDirectory";
	public static final String PROP_BILLINGPRODUCT = "billingProduct";
	public static final String PROP_INVOICELINETEXT = "invoiceLineText";
	
	public String baseDirectory;
	
	
	public void setInfometricBaseDirectory(String directory) {
		baseDirectory = directory;
		reloadTenants();
	}
	
	private void reloadTenants() {
		// TODO: Refresh tenant list based on the contents of the directory
	}
	
	@Override
	public OrderInvoiceOperationResult readInvoices(Date fromDate, Date untilDate, int readLimit) throws Exception {
		
		InfometricTenant it = this.getCurrentTenant()!=null ? this.getCurrentTenant().getNativeBusinessPartner() : null;
		if (it==null) {
			System.out.println("No tenant selected");
			return null;
		}

		BillingFileToInvoiceList bft = new BillingFileToInvoiceList(this, it);
		OrderInvoiceOperationResult result = new OrderInvoiceOperationResult();
		InvoiceList ilist = bft.readAllFiles(1.0);
		result.setSuccessful(true);
		result.setAffectedInvoices(ilist);
		return result;
		
	}

	@Override
	public String getSystemName() {
		return SYSTEM;
	}
	
	@Override
	public BusinessPartner<InfometricTenant> addTenant(String orgNo, String countryCode, String name,
			Properties props) {
		
		try {
		
			BusinessPartner<InfometricTenant> newTenant = super.addTenant(orgNo, countryCode, name, props);
			InfometricTenant it = new InfometricTenant(newTenant);
			if (props!=null) {
				String tenantDir = (String)props.get(InfometricAdapter.PROP_TENANTDIRECTORY);
				if (tenantDir!=null && !tenantDir.startsWith(File.separator)) {
					// prepend basedir
					tenantDir = baseDirectory + File.separator + tenantDir;
				}
				it.setTenantDirectory(tenantDir);
				if (props.get(InfometricAdapter.PROP_BILLINGPRODUCT)!=null) {
					String billingProduct = (String)props.get(InfometricAdapter.PROP_BILLINGPRODUCT);
					String billingText = (String)props.get(InfometricAdapter.PROP_INVOICELINETEXT);
					it.setDefaultMapping(billingProduct, billingText);
				}
			}
			it.savePropertyFile();
			newTenant.setNativeBusinessPartner(it);
			return newTenant;
			
		} catch (Exception ee) {
			ee.printStackTrace();
		}
		return null;
		
	}

	@Override
	public BusinessPartnerList<InfometricTenant> listTenants() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner<Object> lookupBusinessPartner(String key) throws Exception {
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
	public BusinessPartner<InfometricTenant> lookupThisCompanyInformation() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DunningRun<?, ?> lookupDunningRun(String key, Date dueDateUntil) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getClient() {
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
	public Object lookupNativeOrder(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persistNativeOrder(Object order) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Invoice<Object> lookupInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order<Object> lookupOrder(String key) throws Exception {
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
