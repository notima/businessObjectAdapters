package org.notima.businessobjects.adapter.fortnox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.sync.BusinessPartnerSyncProfile;
import org.notima.generic.businessobjects.sync.RemoteObject;
import org.notima.generic.businessobjects.sync.SyncOperation;
import org.notima.generic.businessobjects.sync.SyncProfile;
import org.notima.generic.businessobjects.sync.SyncQuery;
import org.notima.generic.ifacebusinessobjects.ObjectSyncHandler;

/**
 * Syncs a register of business partners (customers or suppliers) with Fortnox. Fortnox keeps
 * customers and suppliers in separate registers, with separate numbers: a partner here that is
 * both gets a link to each ({@link #getRegister()}). The remote key is the customer or supplier
 * number.
 *
 * <p>Listing: the summaries of all partners (one call per page), and when the partners have been
 * synced before, the linked ones changed since then (Fortnox's {@code lastmodified}), so that
 * changes in Fortnox are found without fetching the partners.</p>
 *
 * <p>Payment terms are Fortnox's codes here; they are translated to the local terms by the caller
 * ({@link org.notima.generic.businessobjects.sync.SyncTranslator}).</p>
 */
public abstract class FortnoxPartnerSync implements ObjectSyncHandler<BusinessPartner<?>> {

	/** Fortnox's clock and ours may differ a little. */
	private static final int MODIFIED_MARGIN_MINUTES = 10;

	protected final FortnoxConnector connector;
	private final SyncProfile<BusinessPartner<?>> profile;

	protected FortnoxPartnerSync(FortnoxConnector connector, boolean supplier) {
		this.connector = connector;
		this.profile = new BusinessPartnerSyncProfile(supplier);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class<BusinessPartner<?>> getType() {
		return (Class) BusinessPartner.class;
	}

	/** The customer or supplier number. */
	@Override
	public boolean isRemoteKeyNumber() {
		return true;
	}

	@Override
	public Set<SyncOperation> getSupportedOperations() {
		return Collections.unmodifiableSet(EnumSet.allOf(SyncOperation.class));
	}

	@Override
	public SyncProfile<BusinessPartner<?>> getProfile() {
		return profile;
	}

	@Override
	public List<RemoteObject<BusinessPartner<?>>> list(String orgNo, SyncQuery query) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		Set<String> changed = new HashSet<String>();
		if (query.getModifiedSince()!=null && !query.getLinkedRemoteKeys().isEmpty()) {
			LocalDateTime since = query.getModifiedSince().minusMinutes(MODIFIED_MARGIN_MINUTES);
			for (Summary s : listSummaries(client, FortnoxClient3.lastModifiedFilter(since))) changed.add(s.key);
		}
		List<RemoteObject<BusinessPartner<?>>> result = new ArrayList<RemoteObject<BusinessPartner<?>>>();
		for (Summary s : listSummaries(client, null)) {
			if (s.key==null || s.key.trim().isEmpty()) continue;
			// Linked and unchanged: compared through the fingerprints only
			if (query.getLinkedRemoteKeys().contains(s.key) && !changed.contains(s.key)) continue;
			result.add(new RemoteObject<BusinessPartner<?>>(s.key, s.partner, false));
		}
		return result;
	}

	/** A listed partner: its number and summary. */
	protected static final class Summary {
		final String key;
		final BusinessPartner<?> partner;
		Summary(String key, BusinessPartner<?> partner) {
			this.key = key;
			this.partner = partner;
		}
	}

	/**
	 * @param filter	A Fortnox filter (ie lastmodified), or null for all.
	 * @return			The partners listed.
	 */
	protected abstract List<Summary> listSummaries(FortnoxClient3 client, String filter) throws Exception;

	// ------------------------------------------------------------------

	static String empty(String s) {
		return s!=null && !s.trim().isEmpty() ? s.trim() : null;
	}

	/** For an update: "" clears the field in Fortnox, null would leave it. */
	static String orClear(String s) {
		return s!=null ? s.trim() : "";
	}

	static Location address(BusinessPartner<?> bp) {
		return bp.getAddressOfficial()!=null ? bp.getAddressOfficial() : new Location();
	}

	static String country(BusinessPartner<?> bp) {
		Location a = bp.getAddressOfficial();
		String c = a!=null ? empty(a.getCountryCode()) : null;
		return c!=null ? c : empty(bp.getCountryCode());
	}

	/** The VAT no set on the partner (not one derived from the org no). */
	static String vatNo(BusinessPartner<?> bp) {
		return bp.hasVatNo() ? bp.getVatNo().trim() : null;
	}

	/** A Swedish org no (10 digits) as Fortnox writes it, 556677-8899; others as they are. */
	static String orgNo(String taxId) {
		String t = empty(taxId);
		if (t==null) return null;
		String digits = t.replaceAll("[\\s-]", "");
		if (digits.matches("\\d{10}")) return digits.substring(0, 6) + "-" + digits.substring(6);
		if (digits.matches("\\d{12}")) return digits.substring(0, 8) + "-" + digits.substring(8);
		return t;
	}

	static String requireName(BusinessPartner<?> bp) throws Exception {
		String name = empty(bp.getName());
		if (name==null) throw new Exception("The partner has no name");
		return name;
	}

}
