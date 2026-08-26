package org.notima.businessobjects.adapter.adyen;

import java.util.List;

import org.notima.adyen.AdyenFee;
import org.notima.adyen.AdyenReport;
import org.notima.adyen.AdyenReportRow;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentWriteOff;
import org.notima.generic.businessobjects.PayoutFee;
import org.notima.generic.businessobjects.TransactionReference;
import org.notima.generic.ifacebusinessobjects.PaymentReportRow;
import org.notima.util.LocalDateUtils;

/**
 * Converts a AdyenReport to a payment batch.
 * 
 * @author Daniel Tamm
 *
 */
public class AdyenReportToPaymentBatch {

	// Preserves the previous hardcoded behaviour for channels that don't yet configure
	// destinationReference / destinationReferenceRegex in their adyen.properties file.
	private static final String DEFAULT_DESTINATION_REFERENCE_FIELD = "ExternalInvoiceReference2";
	private static final String DEFAULT_DESTINATION_REFERENCE_REGEX = "^.*?\\.(.*)$";

	private List<PaymentReportRow> rows;
	private PaymentBatch batch;
	private AdyenReport	report;
	private String destinationReferenceField;
	private String destinationReferenceRegex;
	private String sourceReferenceRegex;

	public static AdyenReportToPaymentBatch buildFromReport(AdyenReport report) {
		return buildFromReport(report, null, null);
	}

	/**
	 * @param destinationReferenceField Name of the {@code FortnoxExtendedClient.ReferenceField} to match
	 *        against (e.g. "order", "ExternalInvoiceReference2"). Falls back to the previous hardcoded
	 *        default if {@code null}.
	 * @param destinationReferenceRegex Regex applied to the destination reference before matching.
	 *        Falls back to the previous hardcoded default if {@code null}.
	 */
	public static AdyenReportToPaymentBatch buildFromReport(AdyenReport report, String destinationReferenceField,
			String destinationReferenceRegex) {
		return buildFromReport(report, destinationReferenceField, destinationReferenceRegex, null);
	}

	/**
	 * @param destinationReferenceField Name of the {@code FortnoxExtendedClient.ReferenceField} to match
	 *        against (e.g. "order", "ExternalInvoiceReference2"). Falls back to the previous hardcoded
	 *        default if {@code null}.
	 * @param destinationReferenceRegex Regex applied to the destination reference before matching.
	 *        Falls back to the previous hardcoded default if {@code null}.
	 * @param sourceReferenceRegex Regex applied to the source (Adyen) reference before matching, e.g. to
	 *        strip a prefix. No effect if {@code null}.
	 */
	public static AdyenReportToPaymentBatch buildFromReport(AdyenReport report, String destinationReferenceField,
			String destinationReferenceRegex, String sourceReferenceRegex) {

		AdyenReportToPaymentBatch adyenPaymentBatch = new AdyenReportToPaymentBatch();
		adyenPaymentBatch.rows = report.getReportRows();
		adyenPaymentBatch.report = report;
		adyenPaymentBatch.destinationReferenceField = destinationReferenceField != null
				? destinationReferenceField : DEFAULT_DESTINATION_REFERENCE_FIELD;
		adyenPaymentBatch.destinationReferenceRegex = destinationReferenceRegex != null
				? destinationReferenceRegex : DEFAULT_DESTINATION_REFERENCE_REGEX;
		adyenPaymentBatch.sourceReferenceRegex = sourceReferenceRegex;
		adyenPaymentBatch.build();
		return adyenPaymentBatch;

	}
	
	public PaymentBatch getPaymentBatch() {
		return batch;
	}
	
	private void build() {
	
		if (rows==null) return;
		batch = new PaymentBatch();
		processRows();
		processPayoutFees();
		
	}
	
	
	private void processRows() {
		
		for (PaymentReportRow row : rows) {
			processRow((AdyenReportRow)row);
		}
		
	}
	
	private void processPayoutFees() {
		
		for (PayoutFee f : report.getFeeRows()) {
			batch.addPayoutFee(f);
		}
		
	}
	
	private void processRow(AdyenReportRow row) {
		
		Payment<AdyenReportRow> payment = convertToPayment(row);
		batch.addPayment(payment);
		
	}
	
	private Payment<AdyenReportRow> convertToPayment(AdyenReportRow src) {
		
		Payment<AdyenReportRow> dst = new Payment<AdyenReportRow>();

		dst.setNativePayment(src);
		dst.setCustomerPayment(true);
		dst.setPaymentDate(LocalDateUtils.asDate(report.getSettlementDate()));
		dst.setOriginalAmount(src.getOriginalAmount());
		dst.setAmount(src.getAmount());
		dst.setOrderNo(src.getMerchantReference());
		dst.setComment(src.getModificationReference());
		
		dst.setDestinationSystemReferenceField(destinationReferenceField);
		dst.setDestinationSystemReferenceRegex(destinationReferenceRegex);
		dst.setDestinationSystemReference(src.getPspReference());
		if (sourceReferenceRegex != null) {
			dst.setPreMatchDestinationSystemReferenceRegex(sourceReferenceRegex);
		}

		dst.setClientOrderNo(src.getMerchantReference());
		if (report.getCurrency()!=null) {
			dst.setCurrency(report.getCurrency());
		}

		TransactionReference trxRef = new TransactionReference();
		dst.setTransactionReference(trxRef);
		
		trxRef.setOrderDate(LocalDateUtils.asLocalDate(src.getCreationDate()));
		trxRef.setTransactionId(src.getPspReference());
		if (src.getFees()!=null && src.getFees().size()>0) {
			for (AdyenFee fee : src.getFees()) {
				addFeeToPayment(dst, fee);
			}
		}
		
		return dst;
		
	}
	
	private void addFeeToPayment(Payment<AdyenReportRow> payment, AdyenFee fee) {
		
		PaymentWriteOff pwo = new PaymentWriteOff();
		pwo.setAccountNo(fee.getFeeType().toString());
		pwo.setAmount(fee.getAmount());
		pwo.setComment(fee.getComment());
		payment.addPaymentWriteOff(pwo);
		
	}
	
	
	
}
