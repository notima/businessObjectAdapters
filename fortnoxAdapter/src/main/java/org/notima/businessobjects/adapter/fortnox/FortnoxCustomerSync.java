package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.CustomerSubset;
import org.notima.api.fortnox.entities3.Customers;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.sync.SyncLink;

/**
 * Syncs customers with Fortnox's customer register. A new customer gets the partner's number as
 * customer number if it has one, else Fortnox assigns the next. Only the synced fields are written; fields only Fortnox has (delivery address,
 * price list ...) are left as they are.
 */
public class FortnoxCustomerSync extends FortnoxPartnerSync {

	public static final String REGISTER = "customer";

	public FortnoxCustomerSync(FortnoxConnector connector) {
		super(connector, false);
	}

	@Override
	public String getRegister() {
		return REGISTER;
	}

	@Override
	public boolean appliesTo(BusinessPartner<?> local) {
		return Boolean.TRUE.equals(local.getIsCustomer());
	}

	@Override
	protected List<Summary> listSummaries(FortnoxClient3 client, String filter) throws Exception {
		List<Summary> result = new ArrayList<Summary>();
		Customers customers = client.getCustomers(filter);
		if (customers==null || customers.getCustomerSubset()==null) return result;
		for (CustomerSubset c : customers.getCustomerSubset()) {
			BusinessPartner<Customer> bp = new BusinessPartner<Customer>();
			bp.setName(c.getName());
			bp.setIdentityNo(c.getCustomerNumber());
			bp.setTaxId(c.getOrganisationNumber());
			bp.setIsCustomer(Boolean.TRUE);
			Location a = new Location();
			a.setEmail(c.getEmail());
			a.setPhone(c.getPhone());
			a.setPostal(c.getZipCode());
			a.setCity(c.getCity());
			bp.setAddressOfficial(a);
			result.add(new Summary(c.getCustomerNumber(), bp));
		}
		return result;
	}

	@Override
	public BusinessPartner<?> fetch(String orgNo, String remoteKey) throws Exception {
		Customer c = connector.client(orgNo).getCustomerByCustNo(remoteKey);
		if (c==null) throw new Exception("Customer " + remoteKey + " not found in Fortnox");
		return toCanonical(c);
	}

	@Override
	public SyncLink create(String orgNo, BusinessPartner<?> obj) throws Exception {
		Customer created = connector.client(orgNo).setCustomer(toFortnox(obj, false));
		if (created==null || created.getCustomerNumber()==null) throw new Exception("Fortnox didn't return a customer number");
		return new SyncLink(null, created.getCustomerNumber());
	}

	@Override
	public SyncLink update(String orgNo, BusinessPartner<?> obj, SyncLink link) throws Exception {
		Customer c = toFortnox(obj, true);
		c.setCustomerNumber(link.getRemoteKey());
		if (connector.client(orgNo).updateCustomer(c)==null) {
			throw new Exception("Customer " + link.getRemoteKey() + " not found in Fortnox");
		}
		return link;
	}

	public static BusinessPartner<Customer> toCanonical(Customer c) {
		BusinessPartner<Customer> bp = new BusinessPartner<Customer>();
		bp.setName(c.getName());
		bp.setIdentityNo(empty(c.getCustomerNumber()));
		bp.setTaxId(empty(c.getOrganisationNumber()));
		bp.setVatNo(empty(c.getVATNumber()));
		bp.setIsCustomer(Boolean.TRUE);
		bp.setActive(c.getActive());
		bp.setCompany(!"PRIVATE".equalsIgnoreCase(c.getType()));
		bp.setCurrency(empty(c.getCurrency()));
		bp.setPaymentTermKey(empty(c.getTermsOfPayment()));
		bp.setWebsite(empty(c.getWWW()));
		bp.setVatType(empty(c.getVATType()));
		bp.setComments(empty(c.getComments()));
		bp.setCountryCode(empty(c.getCountryCode()));
		Location a = new Location();
		a.setAddress1(empty(c.getAddress1()));
		a.setAddress2(empty(c.getAddress2()));
		a.setPostal(empty(c.getZipCode()));
		a.setCity(empty(c.getCity()));
		a.setCountryCode(empty(c.getCountryCode()));
		a.setEmail(empty(c.getEmail()));
		a.setPhone(empty(c.getPhone1()));
		bp.setAddressOfficial(a);
		return bp;
	}

	/**
	 * @param update	True for an update: empty fields are sent as empty, so that they are
	 * 					cleared in Fortnox too.
	 */
	public static Customer toFortnox(BusinessPartner<?> bp, boolean update) throws Exception {
		Customer c = new Customer();
		Location a = address(bp);
		c.setName(requireName(bp));
		c.setOrganisationNumber(update ? orClear(orgNo(bp.getTaxId())) : orgNo(bp.getTaxId()));
		c.setVATNumber(update ? orClear(vatNo(bp)) : vatNo(bp));
		c.setAddress1(update ? orClear(a.getAddress1()) : empty(a.getAddress1()));
		c.setAddress2(update ? orClear(a.getAddress2()) : empty(a.getAddress2()));
		c.setZipCode(update ? orClear(a.getPostal()) : empty(a.getPostal()));
		c.setCity(update ? orClear(a.getCity()) : empty(a.getCity()));
		c.setEmail(update ? orClear(a.getEmail()) : empty(a.getEmail()));
		c.setPhone1(update ? orClear(a.getPhone()) : empty(a.getPhone()));
		c.setWWW(update ? orClear(bp.getWebsite()) : empty(bp.getWebsite()));
		// Empty currency, country and payment term: Fortnox's defaults
		c.setCountryCode(country(bp));
		c.setCurrency(empty(bp.getCurrency()));
		c.setTermsOfPayment(empty(bp.getPaymentTermKey()));
		if (bp.getActive()!=null) c.setActive(bp.getActive());
		if (!update) {
			c.setType(bp.isCompany() ? "COMPANY" : null);
			// The partner's number, if it has one; else Fortnox assigns the next
			c.setCustomerNumber(empty(bp.getIdentityNo()));
		}
		return c;
	}

}
