package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.SupplierInvoice;
import org.notima.api.fortnox.entities3.SupplierInvoiceRow;
import org.notima.api.fortnox.entities3.SupplierInvoiceRows;
import org.notima.api.fortnox.entities3.SupplierInvoiceSubset;
import org.notima.api.fortnox.entities3.SupplierInvoices;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.sync.InvoiceSyncProfile;
import org.notima.generic.businessobjects.sync.SyncLink;

/**
 * Syncs supplier invoices with Fortnox (see {@link FortnoxInvoiceSync}). The remote key is
 * Fortnox's number of the invoice (given number); the invoice here keeps its own number. The
 * supplier's number of the invoice is {@link InvoiceSyncProfile#VENDOR_INVOICE_NO_ATTRIBUTE}.
 *
 * <p>Fortnox keeps a supplier invoice as accounting rows: a line here is a row with its account
 * and net amount, and Fortnox adds the rows for the debt and the VAT from the invoice's total and
 * VAT. Reading, those rows are left out, and the lines get the VAT rate of the invoice as a whole.</p>
 */
public class FortnoxSupplierInvoiceSync extends FortnoxInvoiceSync {

	public static final String REGISTER = "supplierinvoice";

	/** Rows Fortnox adds: the debt, the VAT and the rounding. */
	private static final List<String> GENERATED_CODES = java.util.Arrays.asList("TOT", "VAT", "ROV");

	public FortnoxSupplierInvoiceSync(FortnoxConnector connector) {
		super(connector, true);
	}

	@Override
	public String getRegister() {
		return REGISTER;
	}

	@Override
	public boolean appliesTo(Invoice<?> local) {
		return !local.isSalesTransaction();
	}

	@Override
	protected String partnerLinkName() {
		return FortnoxAdapter.SYSTEMNAME + "/" + FortnoxSupplierSync.REGISTER;
	}

	@Override
	protected List<Summary> listSummaries(FortnoxClient3 client, String filter) throws Exception {
		List<Summary> result = new ArrayList<Summary>();
		SupplierInvoices invoices = client.getSupplierInvoices(filter);
		if (invoices==null || invoices.getSupplierInvoiceSubset()==null) return result;
		for (SupplierInvoiceSubset s : invoices.getSupplierInvoiceSubset()) {
			Invoice<Object> inv = new Invoice<Object>();
			inv.setSalesTransaction(false);
			inv.setInvoiceKey(s.getGivenNumber());
			inv.setInvoiceDate(parseDate(s.getInvoiceDate()));
			inv.setDueDate(parseDate(s.getDueDate()));
			inv.setCurrency(empty(s.getCurrency()));
			inv.setGrandTotal(s.getTotal());
			inv.setBusinessPartner(partner(empty(s.getSupplierNumber()), s.getSupplierName(), null, true));
			result.add(new Summary(s.getGivenNumber(), inv, s.isCancelled()));
		}
		return result;
	}

	@Override
	public Invoice<?> fetch(String orgNo, String remoteKey) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		SupplierInvoice src = client.getSupplierInvoice(remoteKey);
		if (src==null) throw new Exception("Supplier invoice " + remoteKey + " not found in Fortnox");
		return toCanonical(client, orgNo, src);
	}

	@Override
	public SyncLink create(String orgNo, Invoice<?> obj) throws Exception {
		SupplierInvoice created = connector.client(orgNo).setSupplierInvoice(toFortnox(obj), true);
		if (created==null || empty(created.getGivenNumber())==null) throw new Exception("Fortnox didn't return a number of the invoice");
		return new SyncLink(FortnoxAdapter.SYSTEMNAME, created.getGivenNumber().trim());
	}

	@Override
	public SyncLink update(String orgNo, Invoice<?> obj, SyncLink link) throws Exception {
		if (!canUpdate(obj)) throw new Exception("The invoice is booked in Fortnox and can't be changed");
		SupplierInvoice dst = toFortnox(obj);
		dst.setGivenNumber(link.getRemoteKey());
		connector.client(orgNo).setSupplierInvoice(dst, false);
		return link;
	}

	// ------------------------------------------------------------------

	public Invoice<?> toCanonical(FortnoxClient3 client, String orgNo, SupplierInvoice src) throws Exception {
		Invoice<SupplierInvoice> dst = new Invoice<SupplierInvoice>();
		dst.setNativeInvoice(src);
		dst.setSalesTransaction(false);
		dst.setInvoiceKey(src.getGivenNumber());
		InvoiceSyncProfile.setVendorInvoiceNo(dst, empty(src.getInvoiceNumber()));
		dst.setInvoiceDate(parseDate(src.getInvoiceDate()));
		dst.setDueDate(parseDate(src.getDueDate()));
		dst.setCurrency(empty(src.getCurrency()));
		dst.setOcr(empty(src.getOCR()));
		dst.setComment(empty(src.getComments()));
		dst.setBusinessPartner(partner(empty(src.getSupplierNumber()), src.getSupplierName(), null, true));
		dst.setBillBpartner(dst.getBusinessPartner());

		double total = nz(src.getTotal());
		double vat = nz(src.getVAT());
		double rate = total - vat != 0 ? Math.round(vat / (total - vat) * 100) : 0;
		dst.setLines(new ArrayList<InvoiceLine>());
		if (src.getSupplierInvoiceRows()!=null && src.getSupplierInvoiceRows().getSupplierInvoiceRow()!=null) {
			for (SupplierInvoiceRow r : src.getSupplierInvoiceRows().getSupplierInvoiceRow()) {
				if (r.getCode()!=null && GENERATED_CODES.contains(r.getCode().trim().toUpperCase())) continue;
				double net = round2(nz(r.getDebit()) - nz(r.getCredit()));
				InvoiceLine l = new InvoiceLine();
				l.setAccountNo(empty(r.getAccount()));
				String text = empty(r.getItemDescription())!=null ? empty(r.getItemDescription()) : empty(r.getTransactionInformation());
				l.setDescription(text);
				l.setQtyEntered(1);
				l.setPriceActual(net);
				l.setTaxPercent(rate);
				l.setLineNet(net);
				l.setTaxAmount(round2(net * rate / 100));
				l.setCostCenter(empty(r.getCostCenter()));
				l.setProject(empty(r.getProject()));
				dst.addInvoiceLine(l);
			}
		}
		dst.setGrandTotal(total);
		dst.setVatTotal(vat);
		dst.setNetTotal(round2(total - vat));
		dst.setOpenAmt(src.getBalance()!=null ? src.getBalance() : total);
		setRemoteVoucher(client, orgNo, dst, src.isBooked(), src.getVoucherSeries(), src.getVoucherNumber(), src.getVoucherYear());
		return dst;
	}

	public SupplierInvoice toFortnox(Invoice<?> src) throws Exception {
		SupplierInvoice dst = new SupplierInvoice();
		dst.setSupplierNumber(partnerNumber(src, "supplier"));
		if (src.getInvoiceDate()==null) throw new Exception("The invoice has no date");
		dst.setInvoiceNumber(InvoiceSyncProfile.vendorInvoiceNo(src));
		dst.setInvoiceDate(formatDate(src.getInvoiceDate()));
		dst.setDueDate(formatDate(src.getDueDate()));
		dst.setCurrency(empty(src.getCurrency()));
		dst.setOCR(empty(src.getOcr()));
		dst.setComments(src.getComment()!=null ? src.getComment().trim() : "");
		dst.setTotal(round2(src.getGrandTotal()));
		dst.setVAT(round2(src.getVatTotal()));
		List<SupplierInvoiceRow> rows = new ArrayList<SupplierInvoiceRow>();
		if (src.getLines()!=null) {
			for (InvoiceLine l : src.getLines()) {
				if (l==null) continue;
				double net = InvoiceSyncProfile.net(l);
				if (net==0) continue;
				String acct = empty(l.getAccountNo());
				if (acct==null || !acct.matches("\\d+")) {
					throw new Exception("A row has no account" + (acct!=null ? " (" + acct + ")" : "")
							+ (l.getDescription()!=null ? ": " + l.getDescription() : ""));
				}
				SupplierInvoiceRow r = new SupplierInvoiceRow();
				r.setAccount(acct);
				if (net > 0) r.setDebit(net);
				else r.setCredit(-net);
				r.setTransactionInformation(empty(l.getDescription()));
				r.setCostCenter(empty(l.getCostCenter()));
				r.setProject(empty(l.getProject()));
				rows.add(r);
			}
		}
		if (rows.isEmpty()) throw new Exception("The invoice has no rows with amounts");
		SupplierInvoiceRows sr = new SupplierInvoiceRows();
		sr.setSupplierInvoiceRow(rows);
		dst.setSupplierInvoiceRows(sr);
		return dst;
	}

}
