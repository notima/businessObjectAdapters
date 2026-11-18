package org.notima.businessobjects.adapter.fortnox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.FinancialYearSubset;
import org.notima.api.fortnox.entities3.FinancialYears;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.settlement.PostedInvoices;
import org.notima.generic.businessobjects.sync.InvoiceSyncProfile;
import org.notima.generic.businessobjects.sync.RemoteObject;
import org.notima.generic.businessobjects.sync.SyncLink;
import org.notima.generic.businessobjects.sync.SyncOperation;
import org.notima.generic.businessobjects.sync.SyncProfile;
import org.notima.generic.businessobjects.sync.SyncQuery;
import org.notima.generic.ifacebusinessobjects.ObjectSyncHandler;

/**
 * Syncs customer or supplier invoices with Fortnox.
 *
 * <ul>
 *   <li>The invoices are posted (booked) in Fortnox. A booked invoice gets the voucher it's posted
 *       on ({@link PostedInvoices#setRemoteVoucher}, with the key of {@link FortnoxVoucherSync}),
 *       and can no longer be changed ({@link #canUpdate}).</li>
 *   <li>New invoices are created unbooked, so they can be changed and synced again until they're
 *       booked in Fortnox.</li>
 *   <li>The partner of an invoice is the customer or supplier it's linked to in Fortnox (its
 *       {@link SyncLink} {@code Fortnox/customer} or {@code Fortnox/supplier}): the partners are
 *       synced first. A listed or fetched invoice's partner has that link, so the caller can find
 *       the partner here.</li>
 *   <li>Listing: the summaries of the invoices of the query's date range, and when the invoices
 *       have been synced before, the linked ones changed since then (Fortnox's
 *       {@code lastmodified}). Cancelled invoices not linked are left out.</li>
 * </ul>
 */
public abstract class FortnoxInvoiceSync implements ObjectSyncHandler<Invoice<?>> {

	/** Fortnox's clock and ours may differ a little. */
	private static final int MODIFIED_MARGIN_MINUTES = 10;

	protected final FortnoxConnector connector;
	private final SyncProfile<Invoice<?>> profile;
	/** First day of each financial year, by org no and Fortnox's id of the year. */
	private final Map<String, LocalDate> yearStarts = new ConcurrentHashMap<String, LocalDate>();

	protected FortnoxInvoiceSync(FortnoxConnector connector, boolean supplier) {
		this.connector = connector;
		this.profile = new InvoiceSyncProfile(supplier);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class<Invoice<?>> getType() {
		return (Class) Invoice.class;
	}

	@Override
	public Set<SyncOperation> getSupportedOperations() {
		return Collections.unmodifiableSet(EnumSet.of(SyncOperation.READ, SyncOperation.CREATE, SyncOperation.UPDATE));
	}

	@Override
	public SyncProfile<Invoice<?>> getProfile() {
		return profile;
	}

	/** An invoice booked in Fortnox can't be changed. */
	@Override
	public boolean canUpdate(Invoice<?> local) {
		return PostedInvoices.remoteVoucher(local)==null;
	}

	/** The name of the links of the partners of these invoices, ie {@code Fortnox/customer}. */
	protected abstract String partnerLinkName();

	@Override
	public List<RemoteObject<Invoice<?>>> list(String orgNo, SyncQuery query) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		String range = range(query);
		Set<String> changed = new HashSet<String>();
		if (query.getModifiedSince()!=null && !query.getLinkedRemoteKeys().isEmpty()) {
			LocalDateTime since = query.getModifiedSince().minusMinutes(MODIFIED_MARGIN_MINUTES);
			String filter = FortnoxClient3.lastModifiedFilter(since);
			for (Summary s : listSummaries(client, range!=null ? range + "&" + filter : filter)) changed.add(s.key);
		}
		List<RemoteObject<Invoice<?>>> result = new ArrayList<RemoteObject<Invoice<?>>>();
		for (Summary s : listSummaries(client, range)) {
			if (s.key==null || s.key.trim().isEmpty()) continue;
			boolean linked = query.getLinkedRemoteKeys().contains(s.key);
			// Linked and unchanged: compared through the fingerprints only
			if (linked && !changed.contains(s.key)) continue;
			if (!linked && s.cancelled) continue;
			result.add(new RemoteObject<Invoice<?>>(s.key, s.invoice, false));
		}
		return result;
	}

	/** A listed invoice: its key, summary and whether cancelled. */
	protected static final class Summary {
		final String key;
		final Invoice<?> invoice;
		final boolean cancelled;
		Summary(String key, Invoice<?> invoice, boolean cancelled) {
			this.key = key;
			this.invoice = invoice;
			this.cancelled = cancelled;
		}
	}

	/**
	 * @param filter	A Fortnox filter (ie fromdate, todate, lastmodified), or null for all.
	 * @return			The invoices listed.
	 */
	protected abstract List<Summary> listSummaries(FortnoxClient3 client, String filter) throws Exception;

	private static String range(SyncQuery q) {
		List<String> parts = new ArrayList<String>();
		if (q.getFromDate()!=null) parts.add("fromdate=" + q.getFromDate());
		if (q.getToDate()!=null) parts.add("todate=" + q.getToDate());
		return parts.isEmpty() ? null : String.join("&", parts);
	}

	// ------------------------------------------------------------------

	/** The partner of a listed or fetched invoice, with its link to Fortnox's register. */
	protected BusinessPartner<?> partner(String number, String name, String taxId, boolean supplier) {
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setName(name);
		bp.setTaxId(empty(taxId));
		bp.setIdentityNo(number);
		if (supplier) bp.setIsVendor(Boolean.TRUE);
		else bp.setIsCustomer(Boolean.TRUE);
		if (number!=null) bp.putSyncLink(new SyncLink(partnerLinkName(), number));
		return bp;
	}

	/**
	 * The partner's number in Fortnox: the remote key of its link.
	 *
	 * @throws Exception	If the partner isn't linked to Fortnox.
	 */
	protected String partnerNumber(Invoice<?> inv, String what) throws Exception {
		BusinessPartner<?> bp = inv.getBusinessPartner();
		if (bp==null) throw new Exception("The invoice has no " + what);
		SyncLink link = bp.getSyncLink(partnerLinkName());
		if (link==null || empty(link.getRemoteKey())==null) {
			throw new Exception("The " + what + " " + (bp.getName()!=null ? bp.getName() + " " : "")
					+ "isn't linked to Fortnox: sync the " + what + "s first");
		}
		return link.getRemoteKey().trim();
	}

	/** Sets where the invoice is posted in Fortnox, if booked. */
	protected void setRemoteVoucher(FortnoxClient3 client, String orgNo, Invoice<?> inv, Boolean booked,
			String series, String number, String yearId) throws Exception {
		if (!Boolean.TRUE.equals(booked) || empty(series)==null || empty(number)==null || empty(yearId)==null) return;
		LocalDate start = yearStart(client, orgNo, yearId.trim());
		if (start==null) return;
		PostedInvoices.setRemoteVoucher(inv, FortnoxAdapter.SYSTEMNAME,
				FortnoxVoucherSync.key(start, series.trim(), Integer.valueOf(number.trim())));
	}

	private LocalDate yearStart(FortnoxClient3 client, String orgNo, String yearId) throws Exception {
		LocalDate start = yearStarts.get(orgNo + "|" + yearId);
		if (start!=null) return start;
		FinancialYears years = client.getFinancialYears();
		if (years!=null && years.getFinancialYearSubset()!=null) {
			for (FinancialYearSubset fy : years.getFinancialYearSubset()) {
				if (fy.getId()!=null && fy.getFromDate()!=null) {
					yearStarts.put(orgNo + "|" + fy.getId(), LocalDate.parse(fy.getFromDate()));
				}
			}
		}
		return yearStarts.get(orgNo + "|" + yearId);
	}

	// ------------------------------------------------------------------

	static String empty(String s) {
		return s!=null && !s.trim().isEmpty() ? s.trim() : null;
	}

	static Date parseDate(String s) throws Exception {
		return empty(s)!=null ? new SimpleDateFormat("yyyy-MM-dd").parse(s.trim()) : null;
	}

	static String formatDate(Date d) {
		return d!=null ? new SimpleDateFormat("yyyy-MM-dd").format(d) : null;
	}

	static double nz(Double d) {
		return d!=null ? d : 0;
	}

	static double round2(double d) {
		return BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

}
