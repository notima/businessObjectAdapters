package org.notima.businessobjects.adapter.fortnox;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.bind.JAXB;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxConstants;
import org.notima.api.fortnox.FortnoxCredentialsProvider;
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.FortnoxInvoiceException;
import org.notima.api.fortnox.FortnoxScopeException;
import org.notima.api.fortnox.LegacyTokenCredentialsProvider;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;
import org.notima.api.fortnox.clients.FortnoxClientInfo;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.clients.FortnoxPropertyFile;
import org.notima.api.fortnox.entities3.Article;
import org.notima.api.fortnox.entities3.ArticleSubset;
import org.notima.api.fortnox.entities3.Articles;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.CustomerSubset;
import org.notima.api.fortnox.entities3.Customers;
import org.notima.api.fortnox.entities3.EmailInformation;
import org.notima.api.fortnox.entities3.FortnoxFile;
import org.notima.api.fortnox.entities3.InvoiceRow;
import org.notima.api.fortnox.entities3.InvoiceRows;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.api.fortnox.entities3.Invoices;
import org.notima.api.fortnox.entities3.OrderRow;
import org.notima.api.fortnox.entities3.PreDefinedAccountSubset;
import org.notima.api.fortnox.entities3.Supplier;
import org.notima.api.fortnox.entities3.VatInfo;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherFileConnection;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.BasicBusinessObjectFactory;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceWriterOptions;
import org.notima.generic.businessobjects.OrderLine;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.PaymentTerm;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.PriceList;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductCategory;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.businessobjects.tax.TaxTool;
import org.notima.generic.ifacebusinessobjects.FactoringReservation;
import org.notima.generic.ifacebusinessobjects.OrderInvoice;
import org.notima.generic.ifacebusinessobjects.OrderInvoiceLine;
import org.notima.util.EmailUtils;
import org.notima.util.LocalDateUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter class to communicate with Fortnox using the Business Objects format.
 * 
 * Copyright 2019-2023 Notima System Integration AB (Sweden)
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

public class FortnoxAdapter extends BasicBusinessObjectFactory<
		FortnoxClient3,
		org.notima.api.fortnox.entities3.Invoice,
		org.notima.api.fortnox.entities3.Order,
		Object,
		org.notima.api.fortnox.entities3.Customer,
		FortnoxClientInfo
		> {
		
	/**
	 * Available lists
	 * @see lookupList(String)
	 */
	public static final String LIST_EXTERNAL_INVOICE_REFERENCE1 = "InvoiceSubset-ExternalInvoiceReference1";	
	public static final String LIST_EXTERNAL_INVOICE_REFERENCE2 = "InvoiceSubset-ExternalInvoiceReference2";
	public static final String LIST_UNPOSTED = "InvoiceSubset-Unposted";
	public static final String LIST_UNPAID = "InvoiceSubset-Unpaid";
	public static final String LIST_UNPAIDOVERDUE = "InvoiceSubset-UnPaidOverDue";
	
	public static final String SYSTEMNAME = "Fortnox";
	
	private FortnoxClient3 fortnoxClient;
	
	private FortnoxClientInfo			currentFortnoxTenant = null;
	private FortnoxCredentialsProvider	currentFortnoxCredentials = null;
	private FortnoxPropertyFile			fortnoxPropertyFile = null;
	private String						currentOrgNo = null;
	
	private FortnoxClientManager clientManager;
	
	private String exportRevenueAccount = null;
	private String defaultRevenueAccount = "3001";
	private String defaultFreightRevenue = "3591";
	private String defaultFreightExportRevenue = "3590";

	private Date	currentDate = null;
	
	private Map<String,VatInfo> revenueAccountMap = null;
	private Map<String, PreDefinedAccountSubset> preDefinedAccountMap = null;

	private FortnoxTaxRateFetcher	taxRateFetcher;
	
	protected static Logger	logger = LoggerFactory.getLogger(FortnoxAdapter.class);
	
	/**
	 * Creates a new Fortnox Adapter using default configuration determined by fortnox4J 
	 * @throws IOException
	 * 
	 */
	public FortnoxAdapter() throws IOException {
		fortnoxClient = new FortnoxClient3(new FortnoxCredentialsProvider("") {
			private final String ERR_MSG = "A Fortnox adapter tried to call the fortnox api without a valid key provider. Make sure to set the tenant of the Fortnox adapter";

			@Override
			public FortnoxCredentials getCredentials() throws Exception {
				throw new Exception(ERR_MSG);
			}

			@Override
			public void setCredentials(FortnoxCredentials key) throws Exception {
				throw new Exception(ERR_MSG);
			}

			@Override
			public void removeAllCredentials() throws Exception {}

			@Override
			public List<FortnoxCredentials> getAllCredentials() throws Exception {
				throw new Exception(ERR_MSG);
			}

			@Override
			public void removeCredential(FortnoxCredentials removeThis) throws Exception {
				throw new Exception(ERR_MSG);			}

			@Override
			public int removeCredentials(List<FortnoxCredentials> removeThese) throws Exception {
				throw new Exception(ERR_MSG);
			}
			
		});
	}
	
	/**
	 * Create an adapter with a pre-initialized client / orgno.
	 * 
	 * @param orgNo
	 * @throws IOException
	 */
	public FortnoxAdapter(String orgNo) throws IOException {
		currentFortnoxTenant = null;
		currentOrgNo = orgNo;
		if (getClientManager()!=null) {
			currentFortnoxTenant = getClientManager().getClientInfoByOrgNo(currentOrgNo);
		}
		currentFortnoxCredentials = new FileCredentialsProvider(currentOrgNo);
		fortnoxClient = new FortnoxClient3(currentFortnoxCredentials);
	}
	
	/**
	 * Used for legacy authentication
	 * 
	 * @param accessToken
	 * @param clientSecret
	 */
	public FortnoxAdapter(String accessToken, String clientSecret) throws Exception {
		
		currentFortnoxCredentials = new LegacyTokenCredentialsProvider(accessToken, clientSecret);
		
		fortnoxClient = new FortnoxClient3(currentFortnoxCredentials);
		FortnoxClientInfo fci = new FortnoxClientInfo();
		try {
			CompanySetting cs = fortnoxClient.getCompanySetting();
			currentOrgNo = cs.getOrganizationNumber();
			fci.setCompanySetting(cs);
		} catch (FortnoxScopeException ee) {
			logger.warn(ee.getMessage());
		}
		fci.setAccessToken(accessToken);
		fci.setClientSecret(clientSecret);
		currentFortnoxTenant = fci;
		
	}
	
	/**
	 * @return 	Returns the native Fortnox Client.
	 */
	public FortnoxClient3 getClient() {
		return fortnoxClient;
	}

	
	/**
	 * Gets client manager for this adapter.
	 * 
	 * @return
	 */
	public FortnoxClientManager getClientManager() {
		if(clientManager == null) 
			clientManager = getServiceReference(FortnoxClientManager.class);
		return clientManager;
	}

	public void setClientManager(FortnoxClientManager clientManager) {
		this.clientManager = clientManager;
	}
	
	public FortnoxPropertyFile getFortnoxPropertyFile() {
		return fortnoxPropertyFile;
	}

	public void setFortnoxPropertyFile(FortnoxPropertyFile fortnoxPropertyFile) {
		this.fortnoxPropertyFile = fortnoxPropertyFile;
	}

	/**
	 * Removes a tenant from a given business object factory.
	 * 
	 * @param orgNo				The orgNo
	 * @param countryCode		The country code
	 * @return	True if the tenant was removed. False if the tenant didn't exist.
	 * @throws Exception if something goes wrong.
	 */
	@Override
	public boolean removeTenant(String orgNo, String countryCode) throws Exception {

		if (clientManager==null) return false;
		
		FortnoxClientInfo fi = clientManager.getClientInfoByOrgNo(orgNo);
		if (fi==null) return false;
		
		if (clientManager.removeClient(fi)) {
			return clientManager.saveClientInfo();
		} else {
			return false;
		}
		
	}

	/**
	 * Adds a new tenant to the adapter.
	 * 
	 * @param orgNo				The org number of the tenant to be added.
	 * @param countryCode		The country code of the tenant to be added.
	 * @param props				Vaild properties are
	 * 							- accessToken
	 * 							- apiCode
	 * 							- clientSecret. If omitted, the manager's default clientSecret is used.
	 * 							- clientId
	 * 
	 * @return		The tenant represented as a business partner.
	 */
	@Override
	public BusinessPartner<FortnoxClientInfo> addTenant(String orgNo, String countryCode, String name, Properties props) {
		
		FortnoxClientInfo fi = null;
		
		// Get properties
		String authorizationCode = null;
		String accessToken = null;
		String clientSecret = null;
		String clientId = null;
		String refreshToken = null;

		if (props!=null) {
			authorizationCode = props.getProperty("apiCode");
			accessToken = props.getProperty("accessToken");
			clientSecret = props.getProperty("clientSecret");
			clientId = props.getProperty("clientId");
			refreshToken = props.getProperty("refreshToken");
		}

		if (clientManager!=null) {
			fi = clientManager.getClientInfoByOrgNo(orgNo);
			if (clientSecret==null || clientSecret.trim().length()==0) {
				clientSecret = clientManager.getDefaultClientSecret();
			}
		}

		BusinessPartner<FortnoxClientInfo> bp = null;

		// TODO Method to add tenant

		if (fi==null) {
			// Create new tenant
			fi = new FortnoxClientInfo();
			fi.setOrgNo(orgNo);
		}
		
		if (name!=null)
			fi.setOrgName(name);
		if (clientSecret!=null)
			fi.setClientSecret(clientSecret);
		if (clientId!=null)
			fi.setClientId(clientId);
		if (accessToken!=null) {
			FortnoxCredentials credentials = new FortnoxCredentials();
			if(authorizationCode != null) {
				credentials.setLegacyToken(accessToken);
				credentials.setAuthorizationCode(authorizationCode);
			} else {
				credentials.setAccessToken(accessToken);
				credentials.setRefreshToken(refreshToken);
			}
			try {
				new FileCredentialsProvider(orgNo).setCredentials(credentials);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try{
			clientManager.updateAndSaveClientInfo(fi);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		bp = convertToBusinessPartnerFromFortnoxClientInfo(fi);
		
		return bp;
	}

	/**
	 * Current date that the factory is working with. If you don't want to work with today's date, 
	 * set this to something else. 
	 * @return
	 */
	public Date getCurrentDate() {
		return currentDate;
	}

	/**
	 * Sets current date. If current date changes, reset the internal revenue account map
	 * 
	 * @param currentDate
	 */
	public void setCurrentDate(Date currentDate) {
		if (this.currentDate==null || !this.currentDate.equals(currentDate)) {
			revenueAccountMap = null;
		}
		this.currentDate = currentDate;
	}

	/**
	 * Gets the current revenue account map
	 * 
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public String getRevenueAccount(String type) throws Exception {
		if (revenueAccountMap==null)
			revenueAccountMap = fortnoxClient.getRevenueAccountMap(currentDate);
		
		VatInfo vatInfo = revenueAccountMap.get(type);
		Integer account = vatInfo!=null ? vatInfo.getDefaultRevenueAccount() : null;
		
		if (account==null) return null;
		if (account == 0) return null;
		
		return account.toString();
	}
	
	/**
	 * Gets corresponding account in chart of accounts.
	 * 
	 * @param type		Should be a Fortnox predefined type.
	 * @return			An account number if defined. Null if not defined.
	 * @throws Exception
	 */
	public String getPredefinedAccount(String type) throws Exception {
		if (preDefinedAccountMap==null) {
			preDefinedAccountMap = fortnoxClient.getPredefinedAccountMap();  
		}
		
		PreDefinedAccountSubset pdas = preDefinedAccountMap.get(type);
		
		if (pdas==null) return null;
		
		Integer account = pdas.getAccount();
		if (account==0) return null;
		
		return account.toString();
	}
	

	/**
	 * Returns the default outVAT (tax due) account.
	 * 
	 * @param taxKey	Must be any of MP1, MP2, MP3 or MP4.
	 * @return		
	 * @throws Exception
	 */
	public String getOutVatAccount(String taxKey) throws Exception {

		String account = getPredefinedAccount("OUTVAT_" + taxKey.toUpperCase());
		return account;
		
	}
	
	/**
	 * Looks up business partner using key.
	 * 
	 * @param	key		The key to lookup business partner
	 * @return		A business partner. If not found, null is returned.
	 * 
	 */
	@Override
	public BusinessPartner<?> lookupBusinessPartner(String key) throws Exception {
		Customer c = fortnoxClient.getCustomerByCustNo(key);
		if (c==null) {
			// Try supplier
			Supplier s = fortnoxClient.getSupplierBySupplierNo(key);
			if (s!=null) {
				return FortnoxConverter.convertToBusinessPartnerFromSupplier(s);
			}
			return null;
		}
		return FortnoxConverter.convertToBusinessPartner(c);
	}

	@Override
	public List<BusinessPartner<?>> lookupAllBusinessPartners() throws Exception {
		
		Customers contacts = fortnoxClient.getCustomers();
		
		List<BusinessPartner<?>> resultList = new ArrayList<BusinessPartner<?>>();
		resultList.addAll(convertCustomerResult(contacts));
		
		return resultList;
		
	}
	
	@Override
	public List<BusinessPartner<Customer>> lookupAllActiveCustomers() throws Exception {
		
		Customers contacts = fortnoxClient.getCustomersActiveOnly();
		
		return convertCustomerResult(contacts);
		
	}
	
	private List<BusinessPartner<Customer>> convertCustomerResult(Customers contacts) throws Exception {
		
		List<BusinessPartner<Customer>> result = new ArrayList<BusinessPartner<Customer>>();
		
		if (contacts!=null && contacts.getCustomerSubset()!=null) {
			for(CustomerSubset c: contacts.getCustomerSubset()) {
				if (isEnrichDataByDefault()) {
					Customer cust = fortnoxClient.getCustomerByCustNo(c.getCustomerNumber());
					result.add(FortnoxConverter.convert(cust));
				} else {
					result.add(FortnoxConverter.convert(c));
				}
			}
		}
		
		return result;
		
	}
	

	public String getDefaultRevenueAccount() {
		return defaultRevenueAccount;
	}

	public void setDefaultRevenueAccount(String defaultRevenueAccount) {
		this.defaultRevenueAccount = defaultRevenueAccount;
	}

	public String getDefaultFreightRevenue() {
		return defaultFreightRevenue;
	}

	public void setDefaultFreightRevenue(String defaultFreightRevenue) {
		this.defaultFreightRevenue = defaultFreightRevenue;
	}

	public String getDefaultFreightExportRevenue() {
		return defaultFreightExportRevenue;
	}

	public void setDefaultFreightExportRevenue(String defaultFreightExportRevenue) {
		this.defaultFreightExportRevenue = defaultFreightExportRevenue;
	}

	public String getExportRevenueAccount() {
		return exportRevenueAccount;
	}

	public void setExportRevenueAccount(String exportRevenueAccount) {
		this.exportRevenueAccount = exportRevenueAccount;
	}

	@Override
	public Invoice<org.notima.api.fortnox.entities3.Invoice> lookupInvoice(String key) throws Exception {
		
		org.notima.api.fortnox.entities3.Invoice src = fortnoxClient.getInvoice(key);
		Invoice<org.notima.api.fortnox.entities3.Invoice> dst = convertToCanonicalInvoice(src);
		dst.setDocumentKey(key);
		return dst;
		
	}

	@Override
	public org.notima.api.fortnox.entities3.Invoice lookupNativeInvoice(String key) throws Exception {
		
		org.notima.api.fortnox.entities3.Invoice raw = fortnoxClient.getInvoice(key);
		return raw;
		
	}
	
	@Override
	public Order<org.notima.api.fortnox.entities3.Order> lookupOrder(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupProduct(String key) throws Exception {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object persist(Object o) throws Exception {

		if (o instanceof org.notima.generic.businessobjects.Invoice) {
			return persistCanonicalInvoice((org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice>)o);
		}
		if (o instanceof org.notima.generic.businessobjects.BusinessPartner) {
			return persistBusinessPartner((org.notima.generic.businessobjects.BusinessPartner)o);
		}
		
		return null;
	}
	
	/**
	 * Saves supplied invoice in Fortnox
	 * 
	 * @param invoice	The invoice to persist.
	 * @return
	 * @throws Exception
	 * @throws FortnoxInvoiceException
	 * @throws FortnoxScopeException
	 */
	protected org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> 
		persistCanonicalInvoice(org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> invoice) 
				throws Exception, FortnoxInvoiceException, FortnoxScopeException {

		// Check that the business partner exists
		Customer cust = fortnoxClient.getCustomerByCustNo(invoice.getBusinessPartner().getIdentityNo());
		if (cust==null) {
			cust = convertFromBusinessPartner(invoice.getBusinessPartner());
			fortnoxClient.setCustomer(cust);
		}
		
		org.notima.api.fortnox.entities3.Invoice dst = convertToFortnoxInvoice(invoice);
	
		if (debugOn) {
			StringWriter stringWriter = new StringWriter();
			JAXB.marshal(dst, stringWriter);
			writeToDebugFile("invoice.xml", stringWriter.getBuffer());
			invoice.setDocumentKey("DEBUG");
			invoice.setInvoiceKey("DEBUG");
		} else {
			dst = fortnoxClient.setInvoice(dst);
			if (dst==null) {
				throw new Exception("Invoice not saved: " + invoice.getDocumentKey());
			}
			// Set document number
			invoice.setDocumentKey(dst.getDocumentNumber());
			invoice.setInvoiceKey(dst.getDocumentNumber());
		}
		invoice.setNativeInvoice(dst);
		
		return invoice;
	}
	
	public org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> persistInvoice(org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> invoice) throws Exception {
		
		return persistCanonicalInvoice(invoice);
	}

	@Override
	public org.notima.api.fortnox.entities3.Invoice persistNativeInvoice(org.notima.api.fortnox.entities3.Invoice invoice) throws Exception {
		
		org.notima.api.fortnox.entities3.Invoice dst = (org.notima.api.fortnox.entities3.Invoice)invoice;
		
		dst = fortnoxClient.setInvoice(dst);

		return dst;
	}
	
	public org.notima.generic.businessobjects.BusinessPartner<Customer> persistBusinessPartner(org.notima.generic.businessobjects.BusinessPartner<Customer> bpartner) throws Exception {
		
		if (!bpartner.getIsVendor()) {
		
			Customer dstContact = convertFromBusinessPartner(bpartner);
	
			// Check if business partner already exists
			Customer checkContact = null;
			if (dstContact.getCustomerNumber()!=null) {
				try {
					checkContact = fortnoxClient.getCustomerByCustNo(dstContact.getCustomerNumber());
				} catch (FortnoxException fe) {
					checkContact = null;
				}
			}
			
			if (checkContact!=null && checkContact.getCustomerNumber()!=null && checkContact.getCustomerNumber().trim().length()>0) {
				// Already exists
				dstContact.setCustomerNumber(checkContact.getCustomerNumber());
			} else {
				// Set customer type
				// 0 = Standard Swedish VAT
				// 1 = Construction VAT
				// 2 = EU customer with VAT number
				// 3 = EU customer without VAT number
				// 4 = Export customer
				// dstContact.setType("0");
				dstContact.setPriceList("A");
				dstContact.setTermsOfPayment("15");
			}
			
			// Save
			dstContact = fortnoxClient.setCustomer(dstContact);
			bpartner.setIdentityNo(dstContact.getCustomerNumber());
		
		} else {

			
			
		}
		
		return bpartner;
	}

	/**
	 * Converts a business partner in common format to Fortnox format
	 * 
	 * @param bpartner		The business partner to convert
	 * @return				A Fortnox representation of business partner (customer)
	 */
	public static org.notima.api.fortnox.entities3.Customer convertFromBusinessPartner(org.notima.generic.businessobjects.BusinessPartner<?> bpartner) {
		
		Customer dstContact = new Customer();
		
		dstContact.setCustomerNumber(bpartner.getIdentityNo());
		dstContact.setOrganisationNumber(bpartner.getTaxId());
		dstContact.setName(bpartner.getName());
		dstContact.setType(bpartner.isCompany() ? Customer.TYPE_COMPANY : Customer.TYPE_PRIVATE);
		Person contact = null;
		List<Person> contacts = bpartner.getContacts();
		if (contacts!=null && contacts.size()>0) {
			contact = contacts.get(0);
		}
		
		if (bpartner.isCompany() && contact!=null) {
			dstContact.setYourReference(contact.getName());
		}
		
		Location loc = bpartner.getAddressOfficial();
		if (loc!=null) {
			dstContact.setAddress1(loc.getAddress1());
			dstContact.setAddress2(loc.getAddress2());
			if (loc.getAddress1()==null || loc.getAddress1().trim().length()==0) {
				dstContact.setAddress1(loc.getStreet());
				if (loc.getHouseNo()!=null && loc.getHouseNo().trim().length()>0) {
					dstContact.setAddress1(dstContact.getAddress1() + " " + loc.getHouseNo());
				}
			}
			if (loc.getCo()!=null && loc.getCo().trim().length()>0) {
				dstContact.setAddress2(dstContact.getAddress1() + 
						(dstContact.getAddress2()!=null && dstContact.getAddress2().trim().length()>0 ? " " + dstContact.getAddress2() : ""));
				dstContact.setAddress1(loc.getCo());
			}
			dstContact.setCity(loc.getCity());
			dstContact.setZipCode(loc.getPostal());
			dstContact.setCountryCode(loc.getCountryCode());
			
			if (EmailUtils.isValidEmail(dstContact.getEmail()))
				dstContact.setEmail(loc.getEmail());
			if (dstContact.getPhone1()==null)
				dstContact.setPhone1(loc.getPhone());
		}

		Location delLoc = bpartner.getAddressShipping();
		if (delLoc!=null) {

			dstContact.setDeliveryAddress1(delLoc.getAddress1());
			dstContact.setDeliveryAddress2(delLoc.getAddress2());
			if (delLoc.getAddress1()==null || delLoc.getAddress1().trim().length()==0) {
				dstContact.setDeliveryAddress1(delLoc.getStreet());
				if (delLoc.getHouseNo()!=null && delLoc.getHouseNo().trim().length()>0) {
					dstContact.setDeliveryAddress1(dstContact.getAddress1() + " " + delLoc.getHouseNo());
				}
			}
			if (delLoc.getCo()!=null && delLoc.getCo().trim().length()>0) {
				dstContact.setDeliveryAddress2(dstContact.getDeliveryAddress1() + 
						(dstContact.getDeliveryAddress2()!=null && dstContact.getDeliveryAddress2().trim().length()>0 ? " " + dstContact.getDeliveryAddress2() : ""));
				dstContact.setDeliveryAddress1(delLoc.getCo());
			}
			dstContact.setCity(delLoc.getCity());
			dstContact.setZipCode(delLoc.getPostal());
			dstContact.setCountryCode(delLoc.getCountryCode());
			
		}
		
		return dstContact;
		
	}
	
	/**
	 * Returns revenue account number for given taxKey and/or taxPercent.
	 * If taxKey can be mapped, this is used first.
	 * If there no match on taxKey, taxPercent is used.
	 * 
	 * Possible taxKeys are:
	 * MP1
	 * MP2
	 * MP3
	 * FTEU
	 * VTEU
	 * E
	 * ES
	 * 
	 * TODO: Convert this function to use ForthoxTaxRateFetcher.
	 * 
	 * @param taxKey		
	 * @param taxPercent
	 * @param taxDomicile	What tax domicile we're dealing with. If not set, SE is assumed.
	 * @return				A revenue account for given taxKey and/or taxPercent.
	 * @throws Exception
	 */
	public String getRevenueAcctNo(String taxKey, Double taxPercent, String taxDomicile) throws Exception {
		
		String accountNo = null;
		if (taxDomicile==null || taxDomicile.trim().length()==0) {
			taxDomicile = FortnoxConstants.DEFAULT_TAX_DOMICILE;
		}

		if ("SE".equals(taxDomicile)) {
		
			if (taxKey!=null) {
				if ("MP1".equals(taxKey)) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_MP1);
				} else if ("MP2".equals(taxKey)) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_MP2);
				} else if ("MP3".equals(taxKey)) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_MP3);
				} else if ("MP0".equals(taxKey)) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_MP0);
				} else if ("FTEU".equals(taxKey)) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_EU_SERVICE);
				} else if ("VTEU".equals(taxKey)) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_EU);
				} else if ("E".equals(taxKey)) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_EXPORT);
				} else if ("ES".equals(taxKey)) {
						accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_EXPORT_SERVICE);
				}
			}
	
			if (accountNo == null && taxPercent!=null) {
	
				// TODO: Make tax percentage check dynamic (and depend on current date)
				if (taxPercent==0)
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_NO_VAT);
				else if (taxPercent==6) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_MP3);
				} else if (taxPercent==12) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_MP2);
				} else if (taxPercent==25) {
					accountNo = getRevenueAccount(FortnoxConstants.ACCT_SALES_MP1);
				}
			}

		} else { 

			FortnoxTaxRateFetcher ftrf = new FortnoxTaxRateFetcher((FortnoxAdapter)this, new TaxSubjectIdentifier(getCurrentTenant().getTaxId()));
			// TODO: Use document date
			List<Tax> validTaxes = ftrf.getValidTaxes(LocalDate.now(), taxDomicile);

			Tax tax = TaxTool.getFirstMatchOnRate(taxPercent, validTaxes);
			accountNo = tax.getDefaultRevenueAccount().getAccountNo();
			
		}
		
		if (accountNo == null) {
			accountNo = defaultRevenueAccount;
		}
		
		return accountNo;
	}
	
	
	/**
	 * Converts from a generic business object to a Fortnox Invoice
	 * 
	 * @param src
	 * @return
	 */
	public org.notima.api.fortnox.entities3.Invoice convertToFortnoxInvoice(org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> src) throws Exception {
		
		org.notima.api.fortnox.entities3.Invoice dst = new org.notima.api.fortnox.entities3.Invoice();

		Customer dstContact = new Customer();

		if (src.getDocumentKey()!=null && src.getDocumentKey().trim().length()>0) 
			dst.setDocumentNumber(src.getDocumentKey());
		
		BusinessPartner<?> bp = src.getBillBpartner()!=null ? (BusinessPartner<?>) src.getBillBpartner() : (BusinessPartner<?>) src.getBusinessPartner();
		if (bp.getIdentityNo()!=null) {
			dstContact.setCustomerNumber(bp.getIdentityNo());
			dstContact.setName(bp.getName());
			dst.setCustomerNumber(bp.getIdentityNo());
			dst.setCustomerName(bp.getName());
		} else {
			throw new Exception("Can not yet create new invoices without the customer already in Fortnox");
		}
		
		Calendar cal = Calendar.getInstance();
		
		// Set to document date if set
		if (src.getDocumentDate()!=null)
			cal.setTime(src.getDocumentDate());
		
		dst.setInvoiceDate(FortnoxClient3.s_dfmt.format(cal.getTime()));
		
		Calendar dueCal = Calendar.getInstance();
		dueCal.setTime(cal.getTime());
		
		if (src.getDueDate()!=null) { 
			dueCal.setTime(src.getDueDate());
		} else {
			dueCal.add(Calendar.DAY_OF_YEAR, 10);		// TODO: Make configurable, default 10 days
		}
		// Calculate number of days
		int days = (int) ((dueCal.getTimeInMillis() - cal.getTimeInMillis())/(1000*3600*24));
		
		if (src.getPaymentTermKey()!=null && src.getPaymentTermKey().trim().length()>0) {
			dst.setTermsOfPayment(src.getPaymentTermKey());
		} else {
		
			if (days<2) {
				dst.setTermsOfPayment("0");	// Must be adaptable
			} else if (days<12) {
				dst.setTermsOfPayment("10");
			} else if (days<17) {
				dst.setTermsOfPayment("15");
			} else if (days<25) {
				dst.setTermsOfPayment("20");
			} else {
				dst.setTermsOfPayment("30");
			}
		}
		
		dst.setDueDate(FortnoxClient3.s_dfmt.format(dueCal.getTime()));
		
		// Set delivery date if set
		if (src.getDeliveryDate()!=null) {
			dst.setDeliveryDate(FortnoxClient3.s_dfmt.format(src.getDeliveryDate()));
		}
		
		dst.setCurrency(src.getCurrency());
		if (src.getPoDocumentNo()!=null) {
			dst.setYourOrderNumber(src.getPoDocumentNo());
		}
		if (src.getOrderKey()!=null) {
			dst.setExternalInvoiceReference2(src.getOrderKey());
		}
		dst.setExternalInvoiceReference1(src.getExternalReference1());
		
		// Accounting dimensions
		if (src.getProject()!=null && src.getProject().trim().length()>0) {
			dst.setProject(src.getProject());
		}
		if (src.getCostCenter()!=null && src.getCostCenter().trim().length()>0) {
			dst.setCostCenter(src.getCostCenter());
		}
		
		// Add invoice row
		InvoiceRows rows;
		rows = new org.notima.api.fortnox.entities3.InvoiceRows();
		dst.setInvoiceRows(rows);		
		List<InvoiceRow> rowList = new ArrayList<InvoiceRow>();
		rows.setInvoiceRow(rowList);
		
		double totalRounding = 0.0;
		
		for (InvoiceLine il : src.getLines()) {
			
			if ("öresavrundning".equals(il.getName())) {
				totalRounding += il.getPriceActual();
				continue;
			}

			addCanonicalInvoiceLineToFortnoxInvoiceRow(src, il, rowList);
			
		}
		
		if (totalRounding!=0) {
			dst.setRoundOff(totalRounding);
		}
		
		if (src.isShowPricesIncludingVAT()) {
			dst.setVATIncluded(Boolean.valueOf(true));
		} else {
			dst.setVATIncluded(Boolean.valueOf(false));
		}
		// Set as not completed as false by default.
		dst.setNotCompleted(Boolean.FALSE);
		
		return dst;
		
	}

	/**
	 * Returns an accounting number for the given invoice and invoice line. If the accountNo is already set 
	 * on the invoice line, no lookup is done.
	 * 
	 * If there is no accountNo on the invoice line, this method tries to determine the account number to use.
	 * 
	 * @param invoice
	 * @param il
	 * @return
	 * @throws Exception
	 * @see {@link #getRevenueAcctNo(String, Double, String)}
	 */
	private String getAccountNo(OrderInvoice invoice, OrderInvoiceLine il) throws Exception {
		
		// Try to set default account number if not set
		if (il.getAccountNo()==null || il.getAccountNo().trim().length()==0) {

			String accountNo = null;
			String taxDomicile = (invoice.getTaxDomicile()==null || invoice.getTaxDomicile().trim().length()==0) ? FortnoxConstants.DEFAULT_TAX_DOMICILE : invoice.getTaxDomicile();
			
			// Lookup article first. Only use article mapping for default tax domicile
			if (FortnoxConstants.DEFAULT_TAX_DOMICILE.equals(taxDomicile) && il.getProductKey()!=null) {
				try {
					Article article = fortnoxClient.getArticleByArticleNo(il.getProductKey());
					if (article!=null && (article.getActive()!=null && article.getActive().booleanValue())) {
						// TODO: Get account depending on type of sales
						accountNo = article.getSalesAccount()!=null ? article.getSalesAccount().toString() : null;
					}
				} catch (Exception ee) {
					logger.warn("Could not lookup product " + il.getProductKey(), ee);
				}
			}
			
			if (accountNo==null) { 
				accountNo = getRevenueAcctNo(il.getTaxKey(), il.getTaxPercent(), invoice.getTaxDomicile());
			}
			
			return accountNo;
				
		} else {
			return il.getAccountNo();
		}
		
	}
	
	/**
	 * 
	 * @param src					The invoice header (source)
	 * @param il					The invoice line to be added
	 * @param fortnoxDstRowList		Initialized Fortnox invoice lines.
	 * @throws Exception
	 */
	public void addCanonicalInvoiceLineToFortnoxInvoiceRow(
			org.notima.generic.businessobjects.Invoice<?> src, 
			InvoiceLine il, 
			List<InvoiceRow> fortnoxDstRowList) throws Exception {
		
		InvoiceRow row;

		row = new InvoiceRow();
		row.setArticleNumber(il.getProductKey());
		row.setDescription(il.getName()!=null ? il.getName() : il.getDescription());
		row.setDeliveredQuantity((double)il.getQtyEntered());
		if (!row.hasDescription()) {
			// Empty description if missing
			row.setDescription(".");
		}
		// Set the accounting number
		row.setAccountNumber(getAccountNo(src, il));
		
		if (il.getProject()!=null && il.getProject().trim().length()>0) 
			row.setProject(il.getProject());
		if (il.getCostCenter()!=null && il.getCostCenter().trim().length()>0)
			row.setCostCenter(il.getCostCenter());
		
		if (il.getPriceActual()!=null) {
			if (il.isPricesIncludeVAT() && !src.isShowPricesIncludingVAT()) {
				row.setPrice((double)il.getPriceActual()-(il.getTaxAmount()/il.getQtyEntered()));
				row.setPrice((Math.round(row.getPrice()*100)/100.0));
			}
			else
				row.setPrice((double)il.getPriceActual());
			row.setVAT((double)il.getTaxPercent());
		} else {
			row.setPrice(null);
			row.setVAT(null);
		}
		row.setUnit(il.getUOM());
		fortnoxDstRowList.add(row);
		
	}
	
	/**
	 * 
	 * @param src					The invoice header (source)
	 * @param il					The invoice line to be added
	 * @param fortnoxDstRowList		Initialized Fortnox invoice lines.
	 * @throws Exception
	 */
	public void addCanonicalOrderLineToFortnoxOrderRow(
			org.notima.generic.businessobjects.Order<?> src, 
			OrderLine il, 
			List<OrderRow> fortnoxDstRowList) throws Exception {
		
		OrderRow row;

		row = new OrderRow();
		row.setArticleNumber(il.getProductKey());
		row.setDescription(il.getName()!=null ? il.getName() : il.getDescription());
		row.setDeliveredQuantity((double)il.getQtyEntered());
		if (row.getDescription()==null || row.getDescription().trim().length()==0) {
			// Empty description if missing
			row.setDescription(".");
		}
		// Set the accounting number
		row.setAccountNumber(getAccountNo(src, il));
		
		if (il.getProject()!=null && il.getProject().trim().length()>0) 
			row.setProject(il.getProject());
		if (il.getCostCenter()!=null && il.getCostCenter().trim().length()>0)
			row.setCostCenter(il.getCostCenter());
		
		if (il.getPriceActual()!=null) {
			if (il.isPricesIncludeVAT() && !src.isShowPricesIncludingVAT()) {
				row.setPrice((double)il.getPriceActual()-(il.getTaxAmount()/il.getQtyEntered()));
				row.setPrice((Math.round(row.getPrice()*100)/100.0));
			}
			else
				row.setPrice((double)il.getPriceActual());
			row.setVAT((double)il.getTaxPercent());
		} else {
			row.setPrice(null);
			row.setVAT(null);
		}
		row.setUnit(il.getUOM());
		fortnoxDstRowList.add(row);
		
	}
	
	
	/**
	 * Converts from a Fortnox Invoice to a generic business object that can be used when 
	 * sending invoice to Svea Ekonomi
	 * 
	 * @param src		The invoice to be converted
	 * @return
	 */
	public static org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> convertToCanonicalInvoice(org.notima.api.fortnox.entities3.Invoice src) throws Exception {
	
		org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> dst = new org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice>();

		dst.setNativeInvoice(src);
		
		dst.setDocumentKey(src.getDocumentNumber());
		dst.setOrderKey(src.getOrderReference());
		dst.setDocumentDate(FortnoxClient3.s_dfmt.parse(src.getInvoiceDate()));
		if (src.getDeliveryDate()!=null && src.getDeliveryDate().trim().length()>0) {
			try {
				dst.setDeliveryDate(FortnoxClient3.s_dfmt.parse(src.getDeliveryDate()));
			} catch (ParseException pe) {
				logger.warn("Can't parse delivery date " + src.getDeliveryDate() + " on invoice " + src.getDocumentNumber());
			}
		}
		dst.setExternalReference1(src.getExternalInvoiceReference1());
		dst.setGrandTotal(src.getTotal());
		// Set open amount
		dst.setOpenAmt(src.getBalance());
		dst.setCurrency(src.getCurrency());
		dst.setDueDate(FortnoxClient3.s_dfmt.parse(src.getDueDate()));
		dst.setOcr(src.getOCR());
		dst.setNetTotal(src.getNet());
		dst.setVatTotal(src.getTotalVAT());
		dst.setPoDocumentNo(src.getExternalInvoiceReference2());
		dst.setPaymentTermKey(src.getTermsOfPayment());
		dst.setComment(src.getRemarks());
		
		if (src.getInvoicePeriodStart()!=null) {
			try {
				dst.setContractStartDate(FortnoxClient3.s_dfmt.parse(src.getInvoicePeriodStart()));
			} catch (Exception e1) {}
		}
		
		if (src.getInvoicePeriodEnd()!=null) {
			try {
				dst.setContractEndDate(FortnoxClient3.s_dfmt.parse(src.getInvoicePeriodEnd()));
			} catch (Exception e2) {}
		}
		
		// First create the business partner
		BusinessPartner<?> dstBp = new BusinessPartner<Customer>();
		dstBp.setName(src.getCustomerName());
		String orgNo = src.getOrganisationNumber(); 
		if (orgNo.length()>11) {
			// Chop of the first two digits
			orgNo = orgNo.substring(2, orgNo.length());
		}
		
		dstBp.setTaxId(orgNo);
		dstBp.setIdentityNo(src.getCustomerNumber());
		dstBp.setEmailInvoice(false);
		dstBp.setLanguage("sv_SE");
		dstBp.setCompany(orgNo.startsWith("55"));
		
		dst.setBusinessPartner(dstBp);
		
		// Set bill person
		Person billPerson = new Person(src.getYourReference());
		dst.setBillPerson(billPerson);
		
		// Create invoice location
		Location billLocation = new Location();
		billLocation.setAddress1(src.getAddress1());
		billLocation.setAddress2(src.getAddress2());
		billLocation.setPostal(src.getZipCode());
		billLocation.setCity(src.getCity());
		// Country mapping (this is weird), why don't we get the country code?
		if ("Sverige".equalsIgnoreCase(src.getCountry())) {
			billLocation.setCountryCode("SE");
		} else {
			billLocation.setCountryCode(src.getCountry());
		}

		dst.setBillLocation(billLocation);
		// TODO: Both ship and bill location are now the same. Should perhaps be adjusted.
		dst.setShipLocation(billLocation);
		
		// Get email information
		EmailInformation einfo = src.getEmailInformation();
		if (einfo!=null) {
			if (einfo.getEmailAddressTo()!=null && einfo.getEmailAddressTo().contains("@")) {
				dst.getBillLocation().setEmail(einfo.getEmailAddressTo());
				dstBp.setEmailInvoice(true);
			}
		}
		
		// Add invoice lines
		InvoiceRows rows = src.getInvoiceRows();
		for (InvoiceRow r : rows.getInvoiceRow()) {
			dst.addInvoiceLine(convert(r, src.isVATIncluded()));
		}
		if(src.getRoundOff() != 0 ){
			InvoiceRow ir = new InvoiceRow();
			ir.setPrice(src.getRoundOff());
			ir.setDescription("öresavrundning");
			ir.setDeliveredQuantity(1.0);
			ir.setVAT(0.0);
			ir.setDeliveredQuantity(1.0);
			dst.addInvoiceLine(convert(ir, false));
		}
		
		return dst;
		
	}
	
	public static org.notima.generic.businessobjects.InvoiceLine convert(org.notima.api.fortnox.entities3.InvoiceRow src, boolean vatIncluded) {
		
		org.notima.generic.businessobjects.InvoiceLine dst = new org.notima.generic.businessobjects.InvoiceLine();
		
		dst.setKey(src.getArticleNumber());
		dst.setName(src.getDescription());
		dst.setPriceActual(src.getPrice());
		dst.setQtyEntered(src.getDeliveredQuantity());
		dst.setTaxPercent(src.getVAT());
		dst.setTaxIncludedInPrice(vatIncluded);
		dst.setUOM(src.getUnit());
		dst.setAccountNo(src.getAccountNumber());
		
		return dst;
		
	}
	
	public static org.notima.generic.businessobjects.BusinessPartner<FortnoxClientInfo> convertToBusinessPartnerFromFortnoxClientInfo(FortnoxClientInfo src) {
		
		if (src==null) return null;
		
		BusinessPartner<FortnoxClientInfo> dst = new BusinessPartner<FortnoxClientInfo>();
		
		dst.setTaxId(src.getOrgNo());
		dst.setName(src.getOrgName());
		// A Fortnox entity is always a company
		dst.setCompany(true);
		dst.setCountryCode(src.getCountryCode());
		List<Person> contacts = new ArrayList<Person>();
		if (src.getContactName()!=null) {
			Person contact = new Person();
			contact.setName(src.getContactName());
			contact.setEmail(src.getContactEmail());
			contacts.add(contact);
		}
		dst.setContacts(contacts);
		
		if (src.getCompanySetting()!=null) {
			// Todo add address etc.
		}
		
		return dst;
		
	}
	
	@Override
	public Product<Object> lookupProductByEan(String ean) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product<Object>> lookupProductByName(String name) throws Exception {
		if (name==null || name.trim().length()==0) {
			return (convertFromArticles(fortnoxClient.getArticles()));
		}
		
		return null;
	}

	
	@SuppressWarnings("unchecked")
	private List<Product<Object>> convertFromArticles(Articles articles) {
		List<Product<Object>> result = new ArrayList<Product<Object>>();		
		if (articles==null) return result;
		
		Product<?> product;
		
		for (ArticleSubset as : articles.getArticleSubset()) {
			product = FortnoxConverter.convertToProduct(as);
			result.add((Product<Object>) product);
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * Available lists
	 * InvoiceSubset-ExternalInvoiceReference2 - The ExternalInvoiceReference will be the key in the map
	 * InvoiceSubset-Unposted
	 * 
	 */
	@Override
	public Map<Object, Object> lookupList(String listName) throws Exception {

		// Invoice subset
		if (LIST_EXTERNAL_INVOICE_REFERENCE1.equalsIgnoreCase(listName)) {
			
			Invoices invoices = fortnoxClient.getInvoices(null);
			
			Map<Object, Object> result = new TreeMap<Object,Object>();
			if (invoices!=null && invoices.getInvoiceSubset()!=null) {
				for (InvoiceSubset is : invoices.getInvoiceSubset()) {
					if (is.getExternalInvoiceReference1()!=null) 
						result.put(is.getExternalInvoiceReference1(), is);
				}
			}
			
			return result;
		}
		
		if (LIST_EXTERNAL_INVOICE_REFERENCE2.equalsIgnoreCase(listName)) {
			
			Invoices invoices = fortnoxClient.getInvoices(null);
			
			Map<Object, Object> result = new TreeMap<Object,Object>();
			if (invoices!=null && invoices.getInvoiceSubset()!=null) {
				for (InvoiceSubset is : invoices.getInvoiceSubset()) {
					if (is.getExternalInvoiceReference2()!=null) 
						result.put(is.getExternalInvoiceReference2(), is);
				}
			}
			
			return result;
		}
		
		
		// Invoice subset - unposted
		if (LIST_UNPOSTED.equalsIgnoreCase(listName)) {
			
			Map<Object,Object> result = getFiltered(FortnoxConstants.FILTER_UNBOOKED);
			
			return result;
		}
		
		// Invoice subset - unpaid overdue
		if (LIST_UNPAIDOVERDUE.equalsIgnoreCase(listName)) {
			
			Map<Object,Object> result = getFiltered(FortnoxConstants.FILTER_UNPAID_OVERDUE);
			
			return result;
		}
		
		if (LIST_UNPAID.equalsIgnoreCase(listName)) {
			
			Map<Object,Object> result = getFiltered(FortnoxConstants.FILTER_UNPAID);
			
			return result;
			
		}
		
		return null;
	}

	/**
	 * Gets invoices using a filter. Handles pagination
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	private Map<Object,Object> getFiltered(String filter) throws Exception {
		
		Invoices invoices = fortnoxClient.getInvoices(filter);
		
		Map<Object, Object> result = new TreeMap<Object,Object>();
		if (invoices!=null && invoices.getInvoiceSubset()!=null) {
			for (InvoiceSubset is : invoices.getInvoiceSubset()) {
				if (is.getDocumentNumber()!=null) 
					result.put(is.getDocumentNumber(), is);
			}
		}
		
		return result;
		
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
	public DunningRun<Customer,org.notima.api.fortnox.entities3.Invoice> lookupDunningRun(String key, Date dueDateUntil) throws Exception {

		FortnoxDunningRunner dunningRunner = new FortnoxDunningRunner(this, key);
		dunningRunner.setUntilDueDate(dueDateUntil);
		dunningRunner.runDunningRun();
		return dunningRunner.getDunningRunResult();
		
	}
	
	/**
	 * Converts "CompanySetting" to "BusinessPartner" with a name, adress and org. number.
	 * 
	 * @param cs
	 * @return
	 */
	public org.notima.generic.businessobjects.BusinessPartner<Customer> convert(org.notima.api.fortnox.entities3.CompanySetting cs){
		BusinessPartner<Customer> creditor = new BusinessPartner<Customer>();
		Location adress = new Location();
		adress.setAddress1(cs.getAdress());
		adress.setCity(cs.getCity());
		adress.setPostal(cs.getZipCode());
		adress.setCountryCode(cs.getCountryCode());
		adress.setPhone(cs.getPhone1());
		adress.setEmail(cs.getEmail());
		
		creditor.setAddressOfficial(adress);
		creditor.setName(cs.getName());
		creditor.setTaxId(cs.getOrganizationNumber());
		return creditor;
	}
	
	/**
	 * Gets the bankgiro string from a "CompanySetting"
	 * 
	 * @param cs
	 * @return
	 */
	public String getBgNo(org.notima.api.fortnox.entities3.CompanySetting cs){
		return cs.getBg();
	}

	@Override
	public org.notima.api.fortnox.entities3.Order lookupNativeOrder(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.notima.api.fortnox.entities3.Order persistNativeOrder(
			org.notima.api.fortnox.entities3.Order order) throws Exception {
		// TODO Auto-generated method stub
		return null;
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
	public BusinessPartner<FortnoxClientInfo> lookupThisCompanyInformation() throws Exception {
		
		CompanySetting cs = fortnoxClient.getCompanySetting();
		BusinessPartner<FortnoxClientInfo> result = new BusinessPartner<FortnoxClientInfo>();
		result.setName(cs.getName());
		result.setTaxId(cs.getOrganizationNumber());
		result.setVatNo(cs.getVatNumber());
		result.setIdentityNo(cs.getDatabaseNumber());
		result.setNativeBusinessPartner(currentFortnoxTenant);
		Location officialAddress = new Location();
		officialAddress.setAddress1(cs.getAdress());
		officialAddress.setCity(cs.getCity());
		officialAddress.setCountryCode(cs.getCountryCode());
		officialAddress.setPostal(cs.getZipCode());
		officialAddress.setEmail(cs.getEmail());
		officialAddress.setPhone(cs.getPhone1());
		result.setAddressOfficial(officialAddress);
		
		Location shipAddress = new Location();
		shipAddress.setAddress1(cs.getVisitAddress());
		shipAddress.setCity(cs.getVisitCity());
		shipAddress.setCountryCode(cs.getVisitCountryCOde());
		shipAddress.setPostal(cs.getVisitZipCode());
		result.setAddressShipping(shipAddress);
		
		return result;
	}

	@Override
	public List<BusinessPartner<?>> lookupBusinessPartners(int maxCount,
			boolean customers, boolean suppliers) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setTenant(String orgNo, String countryCode) throws NoSuchTenantException {

		currentOrgNo = null;
		
		try {
			currentFortnoxCredentials = new FileCredentialsProvider(orgNo);
		} catch (Exception ee) {
			ee.printStackTrace();
		}
		if (currentFortnoxCredentials==null) {
			throw new NoSuchTenantException(orgNo);
		}
		
		currentOrgNo = orgNo;
		fortnoxClient = new FortnoxClient3(currentFortnoxCredentials);
		
		currentFortnoxTenant = null;

		updateCurrentTenant();
	}

	private void updateCurrentTenant() throws NoSuchTenantException {

		if (getClientManager()!=null) {
			currentFortnoxTenant = getClientManager().getClientInfoByOrgNo(currentOrgNo);
			currentFortnoxCredentials.setDefaultClientId(clientManager.getDefaultClientId());
			currentFortnoxCredentials.setDefaultClientSecret(clientManager.getDefaultClientSecret());
			currentTenant = convertToBusinessPartnerFromFortnoxClientInfo(currentFortnoxTenant);
		} else {
			throw new NoSuchTenantException("Can't find client manager while looking for " + currentOrgNo);
		}
		
	}
	
	@Override
	public BusinessPartner<FortnoxClientInfo> getCurrentTenant() {
		
		return convertToBusinessPartnerFromFortnoxClientInfo(currentFortnoxTenant);
		
	}

	@Override
	public BusinessPartnerList<FortnoxClientInfo> listTenants() {

		BusinessPartnerList<FortnoxClientInfo> result = new BusinessPartnerList<FortnoxClientInfo>();
		List<BusinessPartner<FortnoxClientInfo>> listOfBp = new ArrayList<BusinessPartner<FortnoxClientInfo>>();
		result.setBusinessPartner(listOfBp);
		
		BusinessPartner<FortnoxClientInfo> bp = null;
		
		if (clientManager!=null && clientManager.getFortnoxClients()!=null) {
			
			for (FortnoxClientInfo fi : clientManager.getFortnoxClients()) {
				
				bp = convertToBusinessPartnerFromFortnoxClientInfo(fi);
				listOfBp.add(bp);
				
			}
			
		}
		
		if (bp==null && fortnoxClient.hasCredentials()) {
			
			CompanySetting cs;
			try {
				cs = fortnoxClient.getCompanySetting();
				if (cs!=null) {
					bp = createBpFromCompanySetting(cs);
					listOfBp.add(bp);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			
		}
		
		return result;
	}

	private BusinessPartner<FortnoxClientInfo> createBpFromCompanySetting(CompanySetting cs) {
		BusinessPartner<FortnoxClientInfo> bp = new BusinessPartner<FortnoxClientInfo>();
		bp.setName(cs.getName());
		bp.setTaxId(cs.getOrganizationNumber());
		bp.setCountryCode(cs.getCountryCode());
		return bp;
	}
	
	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}

	/**
	 * Writes a list of vouchers to Fortnox
	 * 
	 * The returned list contains the voucher numbers that were set by Fortnox.
	 *
	 * @param	vouchers		The vouchers to write.
	 */
	@Override
	public List<AccountingVoucher> writeVouchers(List<AccountingVoucher> vouchers) throws Exception {
		
		List<AccountingVoucher> result = new ArrayList<AccountingVoucher>();

		if (vouchers==null) return result;
		
		Voucher target;
		Voucher fortnoxVoucher;
		FortnoxConverter conv = new FortnoxConverter();
		for (AccountingVoucher v : vouchers) {
			target = conv.mapFromBusinessObjectVoucher(this, v.getVoucherSeries(), v);
			try {
				fortnoxVoucher = fortnoxClient.setVoucher(target);
				v.setVoucherNo(fortnoxVoucher.getVoucherNumber().toString());
				result.add(v);
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		
		return result;
	}

	@Override
	public OrderInvoiceOperationResult writeInvoices(List<Invoice<?>> canonicalInvoices, OrderInvoiceWriterOptions opts) throws Exception {

		FortnoxInvoiceWriter invoiceWriter = new FortnoxInvoiceWriter(this, opts);
		
		return invoiceWriter.writeInvoices(canonicalInvoices);		
	}
	
	
	/**
	 * Attaches a file to a given voucher.
	 * 
	 * @param		voucher		The voucher to attach to.
	 * @param		fileName	The file to attach.
	 * @return		The fileId of the attached file.
	 * @throws		Exception if something goes wrong.
	 * 
	 */
	@Override
	public String attachFileToVoucher(AccountingVoucher voucher, String fileName) throws Exception {
		
		FortnoxFile ff = fortnoxClient.uploadFile(fileName, FortnoxConstants.INBOX_VOUCHERS);
		VoucherFileConnection vfc =  fortnoxClient.setVoucherFileConnection(ff.getId(), voucher.getVoucherNo(), voucher.getVoucherSeries(), LocalDateUtils.asDate(voucher.getAcctDate()));

		return vfc.getFileId();
		
	}

	private <S> S getServiceReference(Class<S> clazz) {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		if (bundle != null) {
			BundleContext ctx = bundle.getBundleContext();
			ServiceReference<S> reference = ctx
					.getServiceReference(clazz);
			if (reference != null)
				return ctx.getService(reference);
		}
		return null;
    }
	

}
