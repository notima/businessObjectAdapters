package org.notima.businessobjects.adapter.p27;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.Payment.PaymentType;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TransactionReference;
import org.notima.iso20022.camt.CamtAccountId;
import org.notima.iso20022.camt.CamtAccountReport;
import org.notima.iso20022.camt.CamtDocument;
import org.notima.iso20022.camt.CamtEntry;
import org.notima.iso20022.camt.CamtParty;
import org.notima.iso20022.camt.CamtReferredDocument;
import org.notima.iso20022.camt.CamtStructuredRemittance;
import org.notima.iso20022.camt.CamtTransaction;
import org.notima.iso20022.camt.CreditDebit;
import org.notima.util.LocalDateUtils;

/**
 * Converts a camt message (camt.052, camt.053 or camt.054) to payment batches.
 *
 * Each account report results in up to two batches: one with incoming payments (RECEIVABLE)
 * and one with outgoing payments (PAYABLE). Only booked entries are included.
 *
 * Each transaction of an entry becomes a payment. If a transaction pays several documents and
 * states the amount per document, it's split into one payment per document.
 *
 * The reference to match is chosen in this order:
 * <ol>
 * <li>Referred document number (RfrdDocInf/Nb), matched as invoice number.</li>
 * <li>Creditor reference (CdtrRefInf/Ref), matched as OCR.</li>
 * <li>Unstructured remittance (Ustrd), matched as invoice number.</li>
 * </ol>
 * If a destination reference field is set, it's used for all payments.
 *
 * @author Daniel Tamm
 *
 */
public class CamtToPaymentBatch {

	public static final String REFERENCE_FIELD_INVOICE = "invoice";
	public static final String REFERENCE_FIELD_OCR = "OCR";

	private String destinationReferenceField;
	private String destinationReferenceRegex;
	private String sourceReferenceRegex;
	private boolean includePayables = true;

	/**
	 * @param destinationReferenceField	Field to match all references against (e.g. "invoice", "OCR").
	 * 									If null, the field depends on the type of reference.
	 */
	public void setDestinationReferenceField(String destinationReferenceField) {
		this.destinationReferenceField = destinationReferenceField;
	}

	/**
	 * @param destinationReferenceRegex	Regex applied to the destination reference before matching.
	 */
	public void setDestinationReferenceRegex(String destinationReferenceRegex) {
		this.destinationReferenceRegex = destinationReferenceRegex;
	}

	/**
	 * @param sourceReferenceRegex	Regex applied to the reference in the bank file before matching.
	 * 								The first group is used, for instance ^(\d+).*$ to only use the
	 * 								leading digits of "107193 2080".
	 */
	public void setSourceReferenceRegex(String sourceReferenceRegex) {
		this.sourceReferenceRegex = sourceReferenceRegex;
	}

	/**
	 * @param includePayables	If false, outgoing payments are not converted.
	 */
	public void setIncludePayables(boolean includePayables) {
		this.includePayables = includePayables;
	}

	/**
	 * @return	The batches of the document. Batches without payments are left out.
	 */
	public List<PaymentBatch> convert(CamtDocument doc) {
		List<PaymentBatch> result = new ArrayList<PaymentBatch>();
		for (CamtAccountReport report : doc.getReports()) {
			PaymentBatch receivables = createBatch(report, PaymentType.RECEIVABLE);
			PaymentBatch payables = createBatch(report, PaymentType.PAYABLE);
			for (CamtEntry entry : report.getEntries()) {
				if (!entry.isBooked())
					continue;
				// A reversal (e.g. a returned payment) is booked with the opposite indicator
				// and reduces the batch of the original direction.
				boolean incoming = entry.isCredit() != entry.isReversal();
				PaymentBatch batch = incoming ? receivables : payables;
				if (!incoming && !includePayables)
					continue;
				for (Payment<CamtTransaction> p : convertEntry(entry)) {
					p.setCustomerPayment(incoming);
					batch.addPayment(p);
				}
			}
			addIfNotEmpty(result, receivables, report);
			addIfNotEmpty(result, payables, report);
		}
		return result;
	}

	private PaymentBatch createBatch(CamtAccountReport report, PaymentType type) {
		PaymentBatch batch = new PaymentBatch();
		batch.setPaymentType(type);
		BankAccountDetail bad = new BankAccountDetail();
		CamtAccountId acct = report.getAccount();
		if (acct!=null) {
			bad.setIBAN(acct.getIban());
			bad.setAccountNo(acct.getOtherId());
		}
		if (report.getServicer()!=null) {
			bad.setBIC(report.getServicer().getBic());
			bad.setBankName(report.getServicer().getName());
		}
		bad.setCurrency(report.getCurrency());
		batch.setBankAccount(bad);
		return batch;
	}

	private void addIfNotEmpty(List<PaymentBatch> result, PaymentBatch batch, CamtAccountReport report) {
		if (!batch.hasPayments())
			return;
		if (report.getFromDateTime()!=null && report.getToDateTime()!=null) {
			batch.setPeriodFrom(report.getFromDateTime().toLocalDate());
			batch.setPeriodTo(report.getToDateTime().toLocalDate());
		} else {
			batch.setPeriodFrom(LocalDateUtils.asLocalDate(batch.getFirstPaymentDate()));
			batch.setPeriodTo(LocalDateUtils.asLocalDate(batch.getLastPaymentDate()));
		}
		if (batch.getBankAccount().getCurrency()==null) {
			batch.getBankAccount().setCurrency(batch.getPayments().get(0).getCurrency());
		}
		result.add(batch);
	}

	private List<Payment<CamtTransaction>> convertEntry(CamtEntry entry) {
		List<Payment<CamtTransaction>> result = new ArrayList<Payment<CamtTransaction>>();
		int sign = entry.isReversal() ? -1 : 1;

		if (entry.getTransactions().isEmpty()) {
			// No details from the bank, the entry is the payment.
			Payment<CamtTransaction> p = createPayment(entry, null, entry.getAmount(), sign, entry.getAccountServicerReference());
			p.setComment(join(entry.getAdditionalInformation(), entry.getEntryReference()));
			result.add(p);
			return result;
		}

		for (CamtTransaction tx : entry.getTransactions()) {
			String trxId = firstNonNull(tx.getAccountServicerReference(), tx.getEndToEndId(), entry.getAccountServicerReference());
			List<CamtStructuredRemittance> strd = tx.getStructuredRemittance();
			if (isSplittable(tx)) {
				int i = 1;
				for (CamtStructuredRemittance s : strd) {
					Payment<CamtTransaction> p = createPayment(entry, tx, s.getRemittedAmount(), sign, trxId + "-" + i++);
					setReference(p, single(s), new ArrayList<String>());
					result.add(p);
				}
			} else {
				Payment<CamtTransaction> p = createPayment(entry, tx, tx.getAmount(), sign, trxId);
				setReference(p, strd, tx.getUnstructuredRemittance());
				result.add(p);
			}
		}
		return result;
	}

	/**
	 * A transaction is split if it pays more than one document and every document's
	 * amount is stated and they add up to the transaction amount.
	 */
	private boolean isSplittable(CamtTransaction tx) {
		List<CamtStructuredRemittance> strd = tx.getStructuredRemittance();
		if (strd.size()<2 || tx.getAmount()==null)
			return false;
		BigDecimal sum = BigDecimal.ZERO;
		for (CamtStructuredRemittance s : strd) {
			if (s.getRemittedAmount()==null)
				return false;
			sum = sum.add(s.getRemittedAmount());
		}
		return sum.compareTo(tx.getAmount())==0;
	}

	private Payment<CamtTransaction> createPayment(CamtEntry entry, CamtTransaction tx, BigDecimal amount, int sign, String trxId) {

		Payment<CamtTransaction> p = new Payment<CamtTransaction>();
		p.setNativePayment(tx);
		LocalDate date = entry.getBookingDate()!=null ? entry.getBookingDate() : entry.getValueDate();
		p.setPaymentDate(LocalDateUtils.asDate(date));
		if (amount!=null) {
			p.setAmount(amount.doubleValue() * sign);
			p.setOriginalAmount(p.getAmount());
		}
		p.setCurrency(tx!=null && tx.getCurrency()!=null ? tx.getCurrency() : entry.getCurrency());

		if (tx!=null) {
			p.setBusinessPartner(convertParty(tx.getCounterparty()));
			CamtAccountId acct = tx.getCounterpartyAccount();
			if (acct!=null)
				p.setAccountNo(acct.getIdentifier());
		}

		TransactionReference trxRef = new TransactionReference();
		trxRef.setTransactionId(trxId);
		trxRef.setTransactionDate(date);
		p.setTransactionReference(trxRef);
		return p;
	}

	private void setReference(Payment<CamtTransaction> p, List<CamtStructuredRemittance> strd, List<String> ustrd) {

		String reference = null;
		String field = null;
		List<String> allReferences = new ArrayList<String>();

		for (CamtStructuredRemittance s : strd) {
			for (CamtReferredDocument d : s.getReferredDocuments()) {
				if (d.getNumber()==null) continue;
				allReferences.add(d.getNumber());
				if (reference==null) {
					reference = d.getNumber();
					field = REFERENCE_FIELD_INVOICE;
				}
			}
		}
		for (CamtStructuredRemittance s : strd) {
			if (s.getCreditorReference()==null) continue;
			allReferences.add(s.getCreditorReference());
			if (reference==null) {
				reference = s.getCreditorReference();
				field = REFERENCE_FIELD_OCR;
			}
		}
		for (String u : ustrd) {
			allReferences.add(u);
			if (reference==null) {
				reference = u;
				field = REFERENCE_FIELD_INVOICE;
			}
		}

		p.setDestinationSystemReference(reference);
		p.setDestinationSystemReferenceField(destinationReferenceField!=null ? destinationReferenceField :
			(field!=null ? field : REFERENCE_FIELD_INVOICE));
		if (destinationReferenceRegex!=null)
			p.setDestinationSystemReferenceRegex(destinationReferenceRegex);
		if (sourceReferenceRegex!=null)
			p.setPreMatchDestinationSystemReferenceRegex(sourceReferenceRegex);
		if (allReferences.size()>0)
			p.setComment(String.join(" / ", allReferences));
	}

	private BusinessPartner<Object> convertParty(CamtParty src) {
		if (src==null || src.getName()==null) return null;
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setName(src.getName());
		Location loc = new Location();
		loc.setName(src.getName());
		if (src.getStreetName()!=null) {
			loc.setAddress1(join(src.getStreetName(), src.getBuildingNumber()));
		} else if (!src.getAddressLines().isEmpty()) {
			loc.setAddress1(src.getAddressLines().get(0));
			if (src.getAddressLines().size()>1)
				loc.setAddress2(src.getAddressLines().get(1));
		}
		loc.setPostal(src.getPostCode());
		loc.setCity(src.getTownName());
		loc.setCountryCode(src.getCountry());
		bp.setAddressOfficial(loc);
		return bp;
	}

	private static List<CamtStructuredRemittance> single(CamtStructuredRemittance s) {
		List<CamtStructuredRemittance> l = new ArrayList<CamtStructuredRemittance>();
		l.add(s);
		return l;
	}

	private static String firstNonNull(String... values) {
		for (String v : values) {
			if (v!=null) return v;
		}
		return null;
	}

	private static String join(String a, String b) {
		if (a==null) return b;
		if (b==null) return a;
		return a + " " + b;
	}

}
