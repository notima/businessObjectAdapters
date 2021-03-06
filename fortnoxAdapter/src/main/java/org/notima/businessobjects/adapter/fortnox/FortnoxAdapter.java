package org.notima.businessobjects.adapter.fortnox;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.clients.FortnoxClientInfo;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.CustomerSubset;
import org.notima.api.fortnox.entities3.Customers;
import org.notima.api.fortnox.entities3.DefaultDeliveryTypes;
import org.notima.api.fortnox.entities3.EmailInformation;
import org.notima.api.fortnox.entities3.FortnoxFile;
import org.notima.api.fortnox.entities3.InvoiceRow;
import org.notima.api.fortnox.entities3.InvoiceRows;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.api.fortnox.entities3.Invoices;
import org.notima.api.fortnox.entities3.PreDefinedAccountSubset;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherFileConnection;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.BasicBusinessObjectFactory;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.PaymentTerm;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.PriceList;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductCategory;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.util.LocalDateUtils;
import org.notima.generic.ifacebusinessobjects.FactoringReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter class to communicate with Fortnox using the Business Objects format.
 * 
 * Copyright 2019 Notima System Integration AB (Sweden)
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
		org.notima.api.fortnox.entities3.Customer
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
	
	private FortnoxClient3 client;
	
	private FortnoxClientInfo	currentTenant = null;
	
	private FortnoxClientManager clientManager;
	
	private String exportRevenueAccount = null;
	private String defaultRevenueAccount = "3001";
	private String defaultFreightRevenue = "3591";
	private String defaultFreightExportRevenue = "3590";

	private Date	currentDate = null;
	
	private Map<String,Integer> revenueAccountMap = null;
	private Map<String, PreDefinedAccountSubset> preDefinedAccountMap = null;

	protected static Logger	logger = LoggerFactory.getLogger(FortnoxAdapter.class);
	
	/**
	 * Creates a new Fortnox Adapter using default configuration determined by fortnox4J 
	 * 
	 */
	public FortnoxAdapter() {
		client = new FortnoxClient3();
	}
	
	/**
	 * Creates a new business object factory
	 * 
	 * @param accessToken		- AccessToken, the customers token given to the integrator.
	 * @param clientSecret		- ClientSecret, the integrator's secret for the integration.
	 * @throws Exception		If something goes wrong.
	 */
	public FortnoxAdapter(String accessToken, String clientSecret) throws Exception {
		
		// Create Fortnox Client
		client = new FortnoxClient3();
		client.setAccessToken(accessToken, clientSecret);
		
	}
	
	/**
	 * @return 	Returns the native Fortnox Client.
	 */
	public FortnoxClient3 getClient() {
		return client;
	}

	
	/**
	 * Gets client manager for this adapter.
	 * 
	 * @return
	 */
	public FortnoxClientManager getClientManager() {
		return clientManager;
	}

	public void setClientManager(FortnoxClientManager clientManager) {
		this.clientManager = clientManager;
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
	public BusinessPartner<Customer> addTenant(String orgNo, String countryCode, String name, Properties props) {
		
		FortnoxClientInfo fi = null;
		
		// Get properties
		String accessToken = null;
		String apiCode = null;
		String clientSecret = null;
		String clientId = null;

		if (props!=null) {
			accessToken = props.getProperty("accessToken");
			apiCode = props.getProperty("apiCode");
			clientSecret = props.getProperty("clientSecret");
			clientId = props.getProperty("clientId");
		}

		if (clientManager!=null) {
			fi = clientManager.getClientInfoByOrgNo(orgNo);
			if (clientSecret==null || clientSecret.trim().length()==0) {
				clientSecret = clientManager.getDefaultClientSecret();
			}
		}

		BusinessPartner<Customer> bp = null;

		// TODO Method to add tenant

		if (fi==null) {
			// Create new tenant
			fi = new FortnoxClientInfo();
			fi.setOrgNo(orgNo);
		}
		
		if (name!=null)
			fi.setOrgName(name);
		if (accessToken!=null)
			fi.setAccessToken(accessToken);
		if (apiCode!=null)
			fi.setApiCode(apiCode);
		if (clientSecret!=null)
			fi.setClientSecret(clientSecret);
		if (clientId!=null)
			fi.setClientId(clientId);
		

		if (clientManager!=null) {
			try {
				clientManager.validateConnection(fi);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		Customer tenant = new Customer();
		tenant.setOrganisationNumber(fi.getOrgNo());
		tenant.setName(fi.getOrgName());
		tenant.setCountryCode(countryCode);
		
		bp = convert(tenant);
		
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
			revenueAccountMap = client.getRevenueAccountMap(currentDate);
		
		Integer account = revenueAccountMap.get(type);
		
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
			preDefinedAccountMap = client.getPredefinedAccountMap();  
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
	public BusinessPartner<Customer> lookupBusinessPartner(String key) throws Exception {
		Customer c = client.getCustomerByCustNo(key);
		if (c==null) return null;
		return convert(c);
	}

	@Override
	public List<BusinessPartner<Customer>> lookupAllBusinessPartners() throws Exception {
		List<BusinessPartner<Customer>> result = new ArrayList<BusinessPartner<Customer>>();
		
		Customers contacts = client.getCustomers();
		if (contacts!=null && contacts.getCustomerSubset()!=null) {
			for(CustomerSubset c: contacts.getCustomerSubset()) {
				result.add(convert(c));
			}
		}
		while(contacts.getTotalPages()>contacts.getCurrentPage()) {
			// Pause not to exceed call limit
			Thread.sleep(100);
			contacts = client.getCustomers(contacts.getCurrentPage()+1);
			if (contacts!=null && contacts.getCustomerSubset()!=null) {
				for(CustomerSubset c: contacts.getCustomerSubset()) {
					result.add(convert(c));
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
		
		org.notima.api.fortnox.entities3.Invoice src = client.getInvoice(key);
		Invoice<org.notima.api.fortnox.entities3.Invoice> dst = convert(src);
		dst.setDocumentKey(key);
		return dst;
		
	}

	@Override
	public org.notima.api.fortnox.entities3.Invoice lookupNativeInvoice(String key) throws Exception {
		
		org.notima.api.fortnox.entities3.Invoice raw = client.getInvoice(key);
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
			return persist((org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice>)o);
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
	 */
	protected org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> persist(org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> invoice) throws Exception {

		// Check that the business partner exists
		Customer cust = client.getCustomerByCustNo(invoice.getBusinessPartner().getIdentityNo());
		if (cust==null) {
			cust = convert(invoice.getBusinessPartner());
			client.setCustomer(cust);
		}
		
		org.notima.api.fortnox.entities3.Invoice dst = convert(invoice);
	
		dst = client.setInvoice(dst);
		if (dst==null) {
			throw new Exception("Invoice not saved: " + invoice.getDocumentKey());
		}
		// Set document number
		invoice.setDocumentKey(dst.getDocumentNumber());
		invoice.setInvoiceKey(dst.getDocumentNumber());
		invoice.setNativeInvoice(dst);
		
		return invoice;
	}
	
	public org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> persistInvoice(org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> invoice) throws Exception {
		
		return persist(invoice);
	}

	@Override
	public org.notima.api.fortnox.entities3.Invoice persistNativeInvoice(org.notima.api.fortnox.entities3.Invoice invoice) throws Exception {
		
		org.notima.api.fortnox.entities3.Invoice dst = (org.notima.api.fortnox.entities3.Invoice)invoice;
		
		dst = client.setInvoice(dst);

		return dst;
	}
	
	public org.notima.generic.businessobjects.BusinessPartner<Customer> persistBusinessPartner(org.notima.generic.businessobjects.BusinessPartner<Customer> bpartner) throws Exception {
		
		if (!bpartner.getIsVendor()) {
		
			Customer dstContact = convert(bpartner);
	
			// Check if business partner already exists
			Customer checkContact = null;
			if (dstContact.getCustomerNumber()!=null) {
				try {
					checkContact = client.getCustomerByCustNo(dstContact.getCustomerNumber());
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
			dstContact = client.setCustomer(dstContact);
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
	public static org.notima.api.fortnox.entities3.Customer convert(org.notima.generic.businessobjects.BusinessPartner<?> bpartner) {
		
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
			
			if (dstContact.getEmail()==null)
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
	 * @param taxKey		
	 * @param taxPercent
	 * @return				A revenue account for given taxKey and/or taxPercent.
	 * @throws Exception
	 */
	public String getRevenueAcctNo(String taxKey, Double taxPercent) throws Exception {
		
		String accountNo = null;
		
		if (taxKey!=null) {
			if ("MP1".equals(taxKey)) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_MP1);
			} else if ("MP2".equals(taxKey)) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_MP2);
			} else if ("MP3".equals(taxKey)) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_MP3);
			} else if ("MP0".equals(taxKey)) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_MP0);
			} else if ("FTEU".equals(taxKey)) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_EU_SERVICE);
			} else if ("VTEU".equals(taxKey)) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_EU);
			} else if ("E".equals(taxKey)) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_EXPORT);
			} else if ("ES".equals(taxKey)) {
					accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_EXPORT_SERVICE);
			}
		}

		if (accountNo == null && taxPercent!=null) {

			// TODO: Make tax percentage check dynamic (and depend on current date)
			if (taxPercent==0)
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_NO_VAT);
			else if (taxPercent==6) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_MP3);
			} else if (taxPercent==12) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_MP2);
			} else if (taxPercent==25) {
				accountNo = getRevenueAccount(FortnoxClient3.ACCT_SALES_MP1);
			}
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
	public org.notima.api.fortnox.entities3.Invoice convert(org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> src) throws Exception {
		
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
		if (src.getOrderKey()!=null || src.getPoDocumentNo()!=null) {
			dst.setYourOrderNumber(src.getOrderKey());
			dst.setExternalInvoiceReference2(src.getOrderKey());
		}
		
		// Add invoice row
		InvoiceRows rows;
		InvoiceRow row;
		rows = new org.notima.api.fortnox.entities3.InvoiceRows();
		dst.setInvoiceRows(rows);		
		List<InvoiceRow> rowList = new ArrayList<InvoiceRow>();
		rows.setInvoiceRow(rowList);
		
		for (InvoiceLine il : src.getLines()) {
			
			if ("öresavrundning".equals(il.getName())) {
				dst.setRoundOff(il.getPriceActual());
				continue;
			}
			
			row = new InvoiceRow();
			row.setArticleNumber(il.getProductKey());
			row.setDescription(il.getName()!=null ? il.getName() : il.getDescription());
			row.setDeliveredQuantity((double)il.getQtyEntered());
			if (!row.hasDescription()) {
				// Empty description if missing
				row.setDescription(".");
			}
			
			// Try to set default account number if not set
			if (il.getAccountNo()==null || il.getAccountNo().trim().length()==0) {

				String accountNo = getRevenueAcctNo(il.getTaxKey(), il.getTaxPercent());
				
				row.setAccountNumber(accountNo);
					
			} else {
				row.setAccountNumber(il.getAccountNo());
			}
			
			if (il.getPriceActual()!=null) {
				if (il.isPricesIncludeVAT()) {
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
			rowList.add(row);
		}

		// Always set the prices excluding VAT
		dst.setVATIncluded(Boolean.valueOf(false));
		// Set as not completed as false by default.
		dst.setNotCompleted(Boolean.FALSE);
		
		return dst;
		
	}
	
	/**
	 * Converts from a Fortnox Invoice to a generic business object that can be used when 
	 * sending invoice to Svea Ekonomi
	 * 
	 * @param src		The invoice to be converted
	 * @return
	 */
	public static org.notima.generic.businessobjects.Invoice<org.notima.api.fortnox.entities3.Invoice> convert(org.notima.api.fortnox.entities3.Invoice src) throws Exception {
	
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
		dst.setGrandTotal(src.getTotal());
		// Set open amount
		dst.setOpenAmt(src.getBalance());
		dst.setCurrency(src.getCurrency());
		dst.setDueDate(FortnoxClient3.s_dfmt.parse(src.getDueDate()));
		dst.setOcr(src.getOCR());
		dst.setNetTotal(src.getNet());
		dst.setPoDocumentNo(src.getExternalInvoiceReference2());
		dst.setPaymentTermKey(src.getTermsOfPayment());
		
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
			dst.addInvoiceLine(convert(r));
		}
		if(src.getRoundOff() != 0 ){
			InvoiceRow ir = new InvoiceRow();
			ir.setPrice(src.getRoundOff());
			ir.setDescription("öresavrundning");
			ir.setDeliveredQuantity(1.0);
			ir.setVAT(0.0);
			ir.setDeliveredQuantity(1.0);
			dst.addInvoiceLine(convert(ir));
		}
		
		return dst;
		
	}
	
	public static org.notima.generic.businessobjects.InvoiceLine convert(org.notima.api.fortnox.entities3.InvoiceRow src) {
		
		org.notima.generic.businessobjects.InvoiceLine dst = new org.notima.generic.businessobjects.InvoiceLine();
		
		dst.setKey(src.getArticleNumber());
		dst.setName(src.getDescription());
		dst.setPriceActual(src.getPrice());
		dst.setQtyEntered(src.getDeliveredQuantity());
		dst.setTaxPercent(src.getVAT());
		dst.setUOM(src.getUnit());
		dst.setAccountNo(src.getAccountNumber());
		
		return dst;
		
	}
	
	public static org.notima.generic.businessobjects.BusinessPartner<Customer> convert(org.notima.api.fortnox.entities3.Customer src) {
		
		BusinessPartner<Customer> dst = new BusinessPartner<Customer>();
		
		dst.setName(src.getName());
		dst.setIdentityNo(src.getCustomerNumber());
		dst.setTaxId(src.getOrganisationNumber());
		Location loc = new Location();
		dst.setAddressOfficial(loc);
		loc.setEmail(src.getEmail());
		loc.setAddress1(src.getAddress1());
		loc.setAddress2(src.getAddress2());
		loc.setCity(src.getCity());
		loc.setPostal(src.getZipCode());
		loc.setPhone(src.getPhone1());
		loc.setCountryCode(src.getCountryCode());
		dst.setCompany("COMPANY".equalsIgnoreCase(src.getType()));

		// Check default delivery type
		DefaultDeliveryTypes ddt = src.getDefaultDeliveryTypes();
		if (ddt!=null) {
			if ("EMAIL".equalsIgnoreCase(ddt.getInvoice())) {
				dst.setEmailInvoice(true);
			}
		}
		
		// Delivery address if applicable
		if (src.getDeliveryAddress1()!=null && src.getDeliveryAddress1().trim().length()>0) {
			loc = new Location();
			dst.setAddressOfficial(loc);
			loc.setAddress1(src.getDeliveryAddress1());
			loc.setAddress2(src.getDeliveryAddress2());
			loc.setCity(src.getDeliveryCity());
			loc.setPostal(src.getDeliveryZipCode());
			loc.setPhone(src.getDeliveryPhone1());
			loc.setCountryCode(src.getDeliveryCountryCode());
		}
		
		
		return dst;
	}
	
	public static org.notima.generic.businessobjects.BusinessPartner<Customer> convert(org.notima.api.fortnox.entities3.CustomerSubset src) {
		
		BusinessPartner<Customer> dst = new BusinessPartner<Customer>();
		
		dst.setName(src.getName());
		dst.setIdentityNo(src.getCustomerNumber());
		dst.setTaxId(src.getOrganisationNumber());
		Location loc = new Location();
		dst.setAddressOfficial(loc);
		loc.setEmail(src.getEmail());
		loc.setCity(src.getCity());
		loc.setPostal(src.getZipCode());
		loc.setPhone(src.getPhone());
		
		return dst;
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
			
			Invoices invoices = client.getInvoices(null);
			
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
			
			Invoices invoices = client.getInvoices(null);
			
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
			
			Map<Object,Object> result = getFiltered(FortnoxClient3.FILTER_UNBOOKED);
			
			return result;
		}
		
		// Invoice subset - unpaid overdue
		if (LIST_UNPAIDOVERDUE.equalsIgnoreCase(listName)) {
			
			Map<Object,Object> result = getFiltered(FortnoxClient3.FILTER_UNPAID_OVERDUE);
			
			return result;
		}
		
		if (LIST_UNPAID.equalsIgnoreCase(listName)) {
			
			Map<Object,Object> result = getFiltered(FortnoxClient3.FILTER_UNPAID);
			
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
		
		Invoices invoices = client.getInvoices(filter);
		
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
	public DunningRun<Customer,org.notima.api.fortnox.entities3.Invoice> lookupDunningRun(String key) throws Exception {

		DunningRun<Customer,org.notima.api.fortnox.entities3.Invoice> dr = new DunningRun<Customer,org.notima.api.fortnox.entities3.Invoice>();
		
		boolean excludeNegativeOpenAmount = key!=null && "excludeNegativeOpenAmount".equalsIgnoreCase(key);
		
		Map<Object,Object> overdueList = lookupList(LIST_UNPAIDOVERDUE);
		
		Invoice<org.notima.api.fortnox.entities3.Invoice> invoice;
		DunningEntry<org.notima.api.fortnox.entities3.Customer, org.notima.api.fortnox.entities3.Invoice> de;
		
		CompanySetting cs = client.getCompanySetting();
		
		// TODO: Add addInvoice method to DunningRun. Invoices with the same debtor/creditor is located
		//		 with the same dunning entry.
		for (Object o : overdueList.keySet()) {
			invoice = lookupInvoice(o.toString());
			de = new DunningEntry<Customer, org.notima.api.fortnox.entities3.Invoice>();
			de.setCreditor(convert(cs));
			de.setBgNo(getBgNo(cs));
			de.setDebtor((BusinessPartner<Customer>) invoice.getBusinessPartner());
			de.getDebtor().setAddressOfficial(invoice.getBillLocation());
			de.setOcrNo(invoice.getOcr());
			de.setLetterNo(invoice.getDocumentKey());
			invoice.setPaymentTermKey(invoice.getLines().get(0).getName());
			if (invoice!=null) {
				if (excludeNegativeOpenAmount && invoice.getOpenAmt()<0)
					continue;
				de.addInvoice(invoice);
				dr.addDunningEntry(de);
				invoice.setNativeInvoice(null);
			}
		}
		
		return dr;
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
	public BusinessPartner<CompanySetting> lookupThisCompanyInformation() throws Exception {
		
		CompanySetting cs = client.getCompanySetting();
		BusinessPartner<CompanySetting> result = new BusinessPartner<CompanySetting>();
		result.setName(cs.getName());
		result.setTaxId(cs.getOrganizationNumber());
		result.setVatNo(cs.getVatNumber());
		result.setIdentityNo(cs.getDatabaseNumber());
		result.setNativeBusinessPartner(cs);
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
	public List<BusinessPartner<Customer>> lookupBusinessPartners(int maxCount,
			boolean customers, boolean suppliers) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setTenant(String orgNo, String countryCode) throws NoSuchTenantException {

		CompanySetting cs = null;
		
		if (client.hasCredentials()) {

			try {
				cs = client.getCompanySetting();
				if (cs.getOrganizationNumber().equalsIgnoreCase(orgNo)) {
					currentTenant = null;	// Set to make sure current tenant is read using credentials
					return;
				} else {
					
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		if (clientManager!=null) {
			
			FortnoxClientInfo fi = clientManager.getClientInfoByOrgNo(orgNo);
			if (fi==null) {
				throw new NoSuchTenantException("No such tenant " + orgNo);
			} else {
				client.setAccessToken(fi.getAccessToken(), fi.getClientSecret());
				currentTenant = fi;
			}
			
		} else {
			throw new NoSuchTenantException("Can't find a client manager with tenants");
		}
		
	}

	@Override
	public BusinessPartner<Customer> getCurrentTenant() {
		
		if (client.hasCredentials()) {
			// Read from credentials
			CompanySetting cs;
			try {
				cs = client.getCompanySetting();
				BusinessPartner<Customer> bp = new BusinessPartner<Customer>();
				bp.setTaxId(cs.getOrganizationNumber());
				bp.setName(cs.getName());
				bp.setCountryCode(cs.getCountryCode());
				return bp;				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			if (currentTenant==null) return null;
			
			BusinessPartner<Customer> bp = new BusinessPartner<Customer>();
			bp.setTaxId(currentTenant.getOrgNo());
			bp.setName(currentTenant.getOrgName());
			return bp;
		}
		
		return null;
	}

	@Override
	public BusinessPartnerList<Customer> listTenants() {

		BusinessPartnerList<Customer> result = new BusinessPartnerList<Customer>();
		List<BusinessPartner<Customer>> listOfBp = new ArrayList<BusinessPartner<Customer>>();
		result.setBusinessPartner(listOfBp);
		
		BusinessPartner<Customer> bp = null;
		
		if (clientManager!=null && clientManager.getFortnoxClients()!=null) {
			
			for (FortnoxClientInfo fi : clientManager.getFortnoxClients()) {
				
				bp = new BusinessPartner<Customer>();
				bp.setName(fi.getOrgName());
				bp.setTaxId(fi.getOrgNo());
				bp.setCountryCode("SE");
				listOfBp.add(bp);
				
			}
			
		}
		
		if (bp==null && client.hasCredentials()) {
			
			CompanySetting cs;
			try {
				cs = client.getCompanySetting();
				if (cs!=null) {
					bp = new BusinessPartner<Customer>();
					bp.setName(cs.getName());
					bp.setTaxId(cs.getOrganizationNumber());
					bp.setCountryCode(cs.getCountryCode());
					listOfBp.add(bp);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			
		}
		
		return result;
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
				fortnoxVoucher = client.setVoucher(target);
				v.setVoucherNo(fortnoxVoucher.getVoucherNumber().toString());
				result.add(v);
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		
		return result;
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
		
		FortnoxFile ff = client.uploadFile(fileName, FortnoxClient3.INBOX_VOUCHERS);
		VoucherFileConnection vfc =  client.setVoucherFileConnection(ff.getId(), voucher.getVoucherNo(), voucher.getVoucherSeries(), LocalDateUtils.asDate(voucher.getAcctDate()));

		return vfc.getFileId();
		
	}
	

}
