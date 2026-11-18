package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.InvoiceRow;
import org.notima.api.fortnox.entities3.InvoiceRows;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.api.fortnox.entities3.Invoices;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.sync.SyncLink;

/**
 * Syncs customer invoices with Fortnox (see {@link FortnoxInvoiceSync}). The remote key is the
 * invoice number.
 *
 * <p>A new invoice gets its number here if it's a number, else the next in Fortnox's series (and
 * the invoice here gets that number). What Fortnox fills in when creating it (OCR, row accounts,
 * row texts as shortened) is brought back to the invoice here.</p>
 */
public class FortnoxCustomerInvoiceSync extends FortnoxInvoiceSync {

	public static final String REGISTER = "invoice";

	public FortnoxCustomerInvoiceSync(FortnoxConnector connector) {
		super(connector, false);
	}

	@Override
	public String getRegister() {
		return REGISTER;
	}

	@Override
	public boolean appliesTo(Invoice<?> local) {
		return local.isSalesTransaction();
	}

	@Override
	protected String partnerLinkName() {
		return FortnoxAdapter.SYSTEMNAME + "/" + FortnoxCustomerSync.REGISTER;
	}

	@Override
	protected List<Summary> listSummaries(FortnoxClient3 client, String filter) throws Exception {
		List<Summary> result = new ArrayList<Summary>();
		Invoices invoices = client.getInvoices(filter);
		if (invoices==null || invoices.getInvoiceSubset()==null) return result;
		for (InvoiceSubset s : invoices.getInvoiceSubset()) {
			Invoice<Object> inv = new Invoice<Object>();
			inv.setSalesTransaction(true);
			inv.setInvoiceKey(s.getDocumentNumber());
			inv.setInvoiceDate(parseDate(s.getInvoiceDate()));
			inv.setDueDate(parseDate(s.getDueDate()));
			inv.setCurrency(empty(s.getCurrency()));
			inv.setGrandTotal(s.getTotal());
			inv.setBusinessPartner(partner(empty(s.getCustomerNumber()), s.getCustomerName(), null, false));
			result.add(new Summary(s.getDocumentNumber(), inv, s.isCancelled()));
		}
		return result;
	}

	@Override
	public Invoice<?> fetch(String orgNo, String remoteKey) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		org.notima.api.fortnox.entities3.Invoice src = client.getInvoice(remoteKey);
		if (src==null) throw new Exception("Invoice " + remoteKey + " not found in Fortnox");
		return toCanonical(client, orgNo, src);
	}

	@Override
	public SyncLink create(String orgNo, Invoice<?> obj) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		org.notima.api.fortnox.entities3.Invoice dst = toFortnox(obj);
		String no = empty(obj.getInvoiceKey());
		dst.setDocumentNumber(no!=null && no.matches("\\d+") ? no : null);
		org.notima.api.fortnox.entities3.Invoice created = client.createInvoice(dst);
		if (created==null || empty(created.getDocumentNumber())==null) throw new Exception("Fortnox didn't return an invoice number");
		takeAssigned(obj, created);
		return new SyncLink(FortnoxAdapter.SYSTEMNAME, created.getDocumentNumber());
	}

	@Override
	public SyncLink update(String orgNo, Invoice<?> obj, SyncLink link) throws Exception {
		if (!canUpdate(obj)) throw new Exception("The invoice is booked in Fortnox and can't be changed");
		FortnoxClient3 client = connector.client(orgNo);
		org.notima.api.fortnox.entities3.Invoice dst = toFortnox(obj);
		dst.setDocumentNumber(link.getRemoteKey());
		org.notima.api.fortnox.entities3.Invoice updated = client.setInvoice(dst);
		if (updated==null) throw new Exception("Invoice " + link.getRemoteKey() + " not found in Fortnox");
		takeAssigned(obj, updated);
		return link;
	}

	/** What Fortnox assigned or changed: number, OCR, due date, row accounts and texts. */
	public static void takeAssigned(Invoice<?> obj, org.notima.api.fortnox.entities3.Invoice fx) {
		obj.setInvoiceKey(fx.getDocumentNumber());
		if (empty(fx.getOCR())!=null) obj.setOcr(fx.getOCR().trim());
		try {
			if (empty(fx.getDueDate())!=null) obj.setDueDate(parseDate(fx.getDueDate()));
		} catch (Exception ignored) {}
		List<InvoiceRow> rows = fx.getInvoiceRows()!=null && fx.getInvoiceRows().getInvoiceRow()!=null
				? fx.getInvoiceRows().getInvoiceRow() : new ArrayList<InvoiceRow>();
		List<InvoiceLine> lines = obj.getLines();
		if (lines==null || lines.size()!=rows.size()) return;
		for (int i = 0; i < rows.size(); i++) {
			InvoiceLine l = lines.get(i);
			InvoiceRow r = rows.get(i);
			if (empty(l.getAccountNo())==null && empty(r.getAccountNumber())!=null) l.setAccountNo(r.getAccountNumber().trim());
			if (l.getDescription()!=null && r.getDescription()!=null) l.setDescription(r.getDescription());
		}
	}

	// ------------------------------------------------------------------

	public Invoice<?> toCanonical(FortnoxClient3 client, String orgNo, org.notima.api.fortnox.entities3.Invoice src) throws Exception {
		Invoice<org.notima.api.fortnox.entities3.Invoice> dst = new Invoice<org.notima.api.fortnox.entities3.Invoice>();
		dst.setNativeInvoice(src);
		dst.setSalesTransaction(true);
		dst.setInvoiceKey(src.getDocumentNumber());
		dst.setInvoiceDate(parseDate(src.getInvoiceDate()));
		dst.setDueDate(parseDate(src.getDueDate()));
		dst.setCurrency(empty(src.getCurrency()));
		dst.setOcr(empty(src.getOCR()));
		dst.setComment(empty(src.getRemarks()));
		dst.setPoDocumentNo(empty(src.getYourOrderNumber()));
		dst.setPaymentTermKey(empty(src.getTermsOfPayment()));
		if (empty(src.getYourReference())!=null) dst.setBillPerson(new Person(src.getYourReference().trim()));
		if (empty(src.getOurReference())!=null) dst.setOurReference(new Person(src.getOurReference().trim()));
		boolean vatIncluded = Boolean.TRUE.equals(src.isVATIncluded());
		dst.setShowPricesIncludingVAT(vatIncluded);

		BusinessPartner<?> bp = partner(empty(src.getCustomerNumber()), src.getCustomerName(), src.getOrganisationNumber(), false);
		Location a = new Location();
		a.setAddress1(empty(src.getAddress1()));
		a.setAddress2(empty(src.getAddress2()));
		a.setPostal(empty(src.getZipCode()));
		a.setCity(empty(src.getCity()));
		a.setCountryCode(empty(src.getCountry()));
		dst.setBusinessPartner(bp);
		dst.setBillBpartner(bp);
		dst.setBillLocation(a);

		dst.setLines(new ArrayList<InvoiceLine>());
		if (src.getInvoiceRows()!=null && src.getInvoiceRows().getInvoiceRow()!=null) {
			for (InvoiceRow r : src.getInvoiceRows().getInvoiceRow()) {
				InvoiceLine l = new InvoiceLine();
				l.setDescription(r.getDescription());
				l.setAccountNo(empty(r.getAccountNumber()));
				l.setQtyEntered(nz(r.getDeliveredQuantity()));
				l.setPriceActual(nz(r.getPrice()));
				l.setTaxPercent(nz(r.getVAT()));
				l.setUOM(empty(r.getUnit()));
				l.setProductKey(empty(r.getArticleNumber()));
				l.setCostCenter(empty(r.getCostCenter()));
				l.setProject(empty(r.getProject()));
				l.setTaxIncludedInPrice(vatIncluded);
				dst.addInvoiceLine(l);
			}
		}
		dst.calculateGrandTotal();
		// Fortnox's totals, with its rounding
		if (src.getTotal()!=null) dst.setGrandTotal(src.getTotal());
		if (src.getTotalVAT()!=null) dst.setVatTotal(src.getTotalVAT());
		if (src.getNet()!=null) dst.setNetTotal(src.getNet());
		dst.setOpenAmt(src.getBalance()!=null ? src.getBalance() : dst.getGrandTotal());
		setRemoteVoucher(client, orgNo, dst, src.isBooked(), src.getVoucherSeries(), src.getVoucherNumber(), src.getVoucherYear());
		return dst;
	}

	public org.notima.api.fortnox.entities3.Invoice toFortnox(Invoice<?> src) throws Exception {
		org.notima.api.fortnox.entities3.Invoice dst = new org.notima.api.fortnox.entities3.Invoice();
		dst.setCustomerNumber(partnerNumber(src, "customer"));
		if (src.getInvoiceDate()==null) throw new Exception("The invoice has no date");
		dst.setInvoiceDate(formatDate(src.getInvoiceDate()));
		dst.setDueDate(formatDate(src.getDueDate()));
		dst.setCurrency(empty(src.getCurrency()));
		dst.setOCR(empty(src.getOcr()));
		dst.setRemarks(src.getComment()!=null ? src.getComment().trim() : "");
		dst.setYourOrderNumber(src.getPoDocumentNo()!=null ? src.getPoDocumentNo().trim() : "");
		dst.setYourReference(src.getBillPerson()!=null && src.getBillPerson().getName()!=null ? src.getBillPerson().getName().trim() : "");
		dst.setOurReference(src.getOurReference()!=null && src.getOurReference().getName()!=null ? src.getOurReference().getName().trim() : "");
		dst.setVATIncluded(src.isShowPricesIncludingVAT());
		List<InvoiceRow> rows = new ArrayList<InvoiceRow>();
		if (src.getLines()!=null) {
			for (InvoiceLine l : src.getLines()) {
				if (l==null) continue;
				InvoiceRow r = new InvoiceRow();
				r.setDescription(l.getDescription()!=null ? l.getDescription().trim() : null);
				String acct = empty(l.getAccountNo());
				if (acct!=null) {
					if (!acct.matches("\\d+")) throw new Exception("A row has no valid account number: " + acct);
					r.setAccountNumber(acct);
				}
				r.setDeliveredQuantity(l.getQtyEntered());
				r.setPrice(l.getPriceActual()!=null ? l.getPriceActual() : 0.0);
				r.setVAT(l.getTaxPercent());
				r.setUnit(empty(l.getUOM()));
				r.setCostCenter(empty(l.getCostCenter()));
				r.setProject(empty(l.getProject()));
				rows.add(r);
			}
		}
		if (rows.isEmpty()) throw new Exception("The invoice has no rows");
		InvoiceRows ir = new InvoiceRows();
		ir.setInvoiceRow(rows);
		dst.setInvoiceRows(ir);
		return dst;
	}

}
