package org.notima.businessobjects.adapter.p27;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentInstructionWriter;
import org.notima.iso20022.pain.Pain001Writer;
import org.notima.iso20022.pain.PainAccount;
import org.notima.iso20022.pain.PainDocument;
import org.notima.iso20022.pain.PainPaymentInfo;
import org.notima.iso20022.pain.PainTransaction;
import org.notima.util.LocalDateUtils;

/**
 * Writes payments to vendors as an ISO 20022 pain.001.001.03 file for a Swedish bank (see
 * {@link Pain001Writer}).
 *
 * <ul>
 *   <li>The payee's account is its remit-to account: a bankgiro (type {@code BG}), a plusgiro
 *       ({@code PG}), or else its IBAN, with its BIC.</li>
 *   <li>A payment whose reference field is {@value #REFERENCE_FIELD_OCR} is paid with the
 *       reference as OCR number; otherwise the reference, or the invoice number, is the text
 *       to the payee.</li>
 *   <li>The payment's id, without dashes, is its end-to-end id.</li>
 *   <li>The payments are grouped by their date, the requested execution date.</li>
 * </ul>
 */
public class P27PaymentInstructionWriter implements PaymentInstructionWriter {

	/** Reference field of a payment whose reference is an OCR number. */
	public static final String REFERENCE_FIELD_OCR = "OCR";

	private static final DateTimeFormatter MSG_ID = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	@Override
	public String getSystemName() {
		return P27Adapter.SYSTEMNAME;
	}

	@Override
	public String getFormatName() {
		return "ISO 20022 pain.001.001.03";
	}

	@Override
	public String[] getFileExtensions() {
		return new String[] {"xml"};
	}

	@Override
	public List<String> validate(PaymentBatch batch, BusinessPartner<?> initiatingParty) {
		List<String> problems = new ArrayList<String>();
		for (Payment<?> p : payments(batch)) {
			if (p.isCustomerPayment()) problems.add(describe(p) + ": not a payment to a vendor");
			else if (creditorAccount(p) == null) problems.add(describe(p) + ": the payee has no bankgiro, plusgiro or IBAN");
		}
		if (!problems.isEmpty()) return problems;
		return Pain001Writer.validate(toDocument(batch, initiatingParty));
	}

	@Override
	public void writePaymentInstructions(PaymentBatch batch, BusinessPartner<?> initiatingParty, OutputStream out) throws Exception {
		List<String> problems = validate(batch, initiatingParty);
		if (!problems.isEmpty()) throw new IllegalArgumentException(String.join("; ", problems));
		new Pain001Writer().write(toDocument(batch, initiatingParty), out);
	}

	/** The pain.001 document of the batch. */
	PainDocument toDocument(PaymentBatch batch, BusinessPartner<?> initiatingParty) {
		String msgId = "PMT" + MSG_ID.format(LocalDateTime.now());
		String name = initiatingParty != null ? initiatingParty.getName() : null;
		PainDocument doc = new PainDocument()
				.setMessageId(msgId)
				.setInitiatingPartyName(name)
				.setInitiatingPartyId(initiatingParty != null && initiatingParty.getTaxId() != null
						? initiatingParty.getTaxId().replaceAll("\\D", "") : null);

		BankAccountDetail ours = batch.getBankAccount();
		PainAccount debtorAccount = ours != null && ours.getIBAN() != null ? PainAccount.iban(ours.getIBAN(), ours.getBIC()) : null;

		Map<LocalDate, PainPaymentInfo> byDate = new TreeMap<LocalDate, PainPaymentInfo>();
		for (Payment<?> p : payments(batch)) {
			LocalDate date = LocalDateUtils.asLocalDate(p.getPaymentDate());
			PainPaymentInfo info = byDate.get(date);
			if (info == null) {
				info = new PainPaymentInfo()
						.setPaymentInfoId(msgId + "-" + (byDate.size() + 1))
						.setRequestedExecutionDate(date)
						.setDebtorName(name)
						.setDebtorAccount(debtorAccount);
				byDate.put(date, info);
			}
			Double amt = p.getOriginalAmount();
			BusinessPartner<?> payee = p.getBusinessPartner();
			String ref = p.getDestinationSystemReferenceRaw() != null ? p.getDestinationSystemReferenceRaw() : p.getInvoiceNo();
			boolean ocr = REFERENCE_FIELD_OCR.equalsIgnoreCase(p.getDestinationSystemReferenceField());
			info.addTransaction(new PainTransaction()
					.setEndToEndId(endToEndId(p))
					.setAmount(amt != null ? BigDecimal.valueOf(amt) : null)
					.setCurrency(p.getCurrency() != null ? p.getCurrency() : (ours != null ? ours.getCurrency() : null))
					.setCreditorName(payee != null ? payee.getName() : null)
					.setCreditorAccount(creditorAccount(p))
					.setCreditorReference(ocr ? ref : null)
					.setRemittanceText(ocr ? null : ref));
		}
		for (PainPaymentInfo info : byDate.values()) doc.addPaymentInfo(info);
		return doc;
	}

	/** The end-to-end id of a payment: its id without dashes, at most 35 characters. */
	public static String endToEndId(Payment<?> p) {
		String id = p.getPaymentId() != null ? p.getPaymentId().replace("-", "") : null;
		return id != null && id.length() > 35 ? id.substring(0, 35) : id;
	}

	/** The payee's account: its bankgiro, plusgiro or IBAN; null if it has none. */
	static PainAccount creditorAccount(Payment<?> p) {
		BusinessPartner<?> bp = p.getBusinessPartner();
		if (bp == null) return null;
		String type = bp.getRemitToAccountType() != null ? bp.getRemitToAccountType().trim().toUpperCase() : "";
		String account = bp.getRemitToAccount();
		if (account != null && !account.trim().isEmpty()) {
			if ("BG".equals(type)) return PainAccount.bankgiro(account);
			if ("PG".equals(type)) return PainAccount.plusgiro(account);
		}
		if (bp.getRemitToIBAN() != null && !bp.getRemitToIBAN().trim().isEmpty()) {
			return PainAccount.iban(bp.getRemitToIBAN(), bp.getRemitToBIC());
		}
		return null;
	}

	private static List<Payment<?>> payments(PaymentBatch batch) {
		return batch != null && batch.getPayments() != null ? batch.getPayments() : new ArrayList<Payment<?>>();
	}

	private static String describe(Payment<?> p) {
		BusinessPartner<?> bp = p.getBusinessPartner();
		return (bp != null && bp.getName() != null ? bp.getName() : "?") + " " + p.getOriginalAmount();
	}

}
