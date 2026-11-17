package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Supplier;
import org.notima.api.fortnox.entities3.SupplierSubset;
import org.notima.api.fortnox.entities3.Suppliers;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.sync.SyncLink;

/**
 * Syncs suppliers with Fortnox's supplier register, including where to pay them (bankgiro or
 * plusgiro, IBAN and BIC). A new supplier gets the partner's number as supplier number if it has
 * one and isn't a customer too, else Fortnox assigns the next.
 */
public class FortnoxSupplierSync extends FortnoxPartnerSync {

	public static final String REGISTER = "supplier";

	public FortnoxSupplierSync(FortnoxConnector connector) {
		super(connector, true);
	}

	@Override
	public String getRegister() {
		return REGISTER;
	}

	@Override
	public boolean appliesTo(BusinessPartner<?> local) {
		return Boolean.TRUE.equals(local.getIsVendor());
	}

	@Override
	protected List<Summary> listSummaries(FortnoxClient3 client, String filter) throws Exception {
		List<Summary> result = new ArrayList<Summary>();
		Suppliers suppliers = client.getSuppliers(filter);
		if (suppliers==null || suppliers.getSupplierSubset()==null) return result;
		for (SupplierSubset s : suppliers.getSupplierSubset()) {
			BusinessPartner<Supplier> bp = canonical(s);
			bp.getAddressOfficial().setPhone(s.getPhone());
			result.add(new Summary(s.getSupplierNumber(), bp));
		}
		return result;
	}

	@Override
	public BusinessPartner<?> fetch(String orgNo, String remoteKey) throws Exception {
		Supplier s = connector.client(orgNo).getSupplierBySupplierNo(remoteKey);
		if (s==null) throw new Exception("Supplier " + remoteKey + " not found in Fortnox");
		return toCanonical(s);
	}

	@Override
	public SyncLink create(String orgNo, BusinessPartner<?> obj) throws Exception {
		Supplier created = connector.client(orgNo).setSupplier(toFortnox(obj, false), true);
		if (created==null || created.getSupplierNumber()==null) throw new Exception("Fortnox didn't return a supplier number");
		return new SyncLink(null, created.getSupplierNumber());
	}

	@Override
	public SyncLink update(String orgNo, BusinessPartner<?> obj, SyncLink link) throws Exception {
		Supplier s = toFortnox(obj, true);
		s.setSupplierNumber(link.getRemoteKey());
		if (connector.client(orgNo).setSupplier(s, false)==null) {
			throw new Exception("Supplier " + link.getRemoteKey() + " not found in Fortnox");
		}
		return link;
	}

	/** What the listing and the whole supplier have in common. */
	private static BusinessPartner<Supplier> canonical(SupplierSubset s) {
		BusinessPartner<Supplier> bp = new BusinessPartner<Supplier>();
		bp.setName(s.getName());
		bp.setIdentityNo(empty(s.getSupplierNumber()));
		bp.setTaxId(empty(s.getOrganisationNumber()));
		bp.setIsVendor(Boolean.TRUE);
		bp.setCompany(true);
		bp.setCurrency(empty(s.getCurrency()));
		bp.setPaymentTermKey(empty(s.getTermsOfPayment()));
		bp.setCountryCode(empty(s.getCountryCode()));
		if (empty(s.getBG())!=null) {
			bp.setRemitToAccountType("BG");
			bp.setRemitToAccount(empty(s.getBG()));
		} else if (empty(s.getPG())!=null) {
			bp.setRemitToAccountType("PG");
			bp.setRemitToAccount(empty(s.getPG()));
		}
		bp.setRemitToIBAN(empty(s.getIBAN()));
		bp.setRemitToBIC(empty(s.getBIC()));
		Location a = new Location();
		a.setAddress1(empty(s.getAddress1()));
		a.setAddress2(empty(s.getAddress2()));
		a.setPostal(empty(s.getZipCode()));
		a.setCity(empty(s.getCity()));
		a.setCountryCode(empty(s.getCountryCode()));
		a.setEmail(empty(s.getEmail()));
		bp.setAddressOfficial(a);
		return bp;
	}

	public static BusinessPartner<Supplier> toCanonical(Supplier s) {
		BusinessPartner<Supplier> bp = canonical(s);
		bp.setVatNo(empty(s.getVATNumber()));
		bp.setActive(s.getActive());
		bp.setWebsite(empty(s.getWww()));
		bp.setComments(empty(s.getComments()));
		bp.getAddressOfficial().setPhone(empty(s.getPhone1()));
		return bp;
	}

	/**
	 * @param update	True for an update: empty fields are sent as empty, so that they are
	 * 					cleared in Fortnox too.
	 */
	public static Supplier toFortnox(BusinessPartner<?> bp, boolean update) throws Exception {
		Supplier s = new Supplier();
		Location a = address(bp);
		s.setName(requireName(bp));
		s.setOrganisationNumber(update ? orClear(orgNo(bp.getTaxId())) : orgNo(bp.getTaxId()));
		s.setVATNumber(update ? orClear(vatNo(bp)) : vatNo(bp));
		s.setAddress1(update ? orClear(a.getAddress1()) : empty(a.getAddress1()));
		s.setAddress2(update ? orClear(a.getAddress2()) : empty(a.getAddress2()));
		s.setZipCode(update ? orClear(a.getPostal()) : empty(a.getPostal()));
		s.setCity(update ? orClear(a.getCity()) : empty(a.getCity()));
		s.setEmail(update ? orClear(a.getEmail()) : empty(a.getEmail()));
		s.setPhone1(update ? orClear(a.getPhone()) : empty(a.getPhone()));
		s.setWww(update ? orClear(bp.getWebsite()) : empty(bp.getWebsite()));
		s.setCountryCode(country(bp));
		s.setCurrency(empty(bp.getCurrency()));
		s.setTermsOfPayment(empty(bp.getPaymentTermKey()));
		String type = bp.getRemitToAccountType()!=null ? bp.getRemitToAccountType().trim().toUpperCase() : "";
		String account = empty(bp.getRemitToAccount());
		String bg = account!=null && !"PG".equals(type) ? account : null;
		String pg = account!=null && "PG".equals(type) ? account : null;
		s.setBG(update ? orClear(bg) : bg);
		s.setPG(update ? orClear(pg) : pg);
		s.setIBAN(update ? orClear(bp.getRemitToIBAN()) : empty(bp.getRemitToIBAN()));
		s.setBIC(update ? orClear(bp.getRemitToBIC()) : empty(bp.getRemitToBIC()));
		if (bp.getActive()!=null) s.setActive(bp.getActive());
		// The partner's number, unless it's a customer too (then it's the customer number, and
		// the supplier register has its own series): Fortnox assigns the next
		if (!update && !Boolean.TRUE.equals(bp.getIsCustomer())) s.setSupplierNumber(empty(bp.getIdentityNo()));
		return s;
	}

}
