package org.notima.businessobjects.adapter.ratepay;

import java.util.List;

import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentWriteOff;
import org.notima.generic.businessobjects.TransactionReference;
import org.notima.ratepay.RatepayFee;
import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportRow;
import org.notima.util.LocalDateUtils;

/**
 * Converts a RatepayReport to a payment batch.
 * 
 * @author Daniel Tamm
 *
 */
public class RatepayToPaymentBatch {

	private List<RatepayReportRow> rows;
	private PaymentBatch batch;
	private RatepayReport	report;
	
	public static RatepayToPaymentBatch buildFromReport(RatepayReport report) {
		
		RatepayToPaymentBatch ratepay = new RatepayToPaymentBatch();
		ratepay.rows = report.getReportRows();
		ratepay.report = report; 
		ratepay.build();
		return ratepay;
		
	}
	
	public PaymentBatch getPaymentBatch() {
		return batch;
	}
	
	private void build() {
	
		if (rows==null) return;
		batch = new PaymentBatch();
		processRows();
		
	}
	
	private void processRows() {
		
		for (RatepayReportRow row : rows) {
			processRow(row);
		}
		
	}
	
	private void processRow(RatepayReportRow row) {
		
		Payment<RatepayReportRow> payment = convertToPayment(row);
		batch.addPayment(payment);
		
	}
	
	private Payment<RatepayReportRow> convertToPayment(RatepayReportRow src) {
		
		Payment<RatepayReportRow> dst = new Payment<RatepayReportRow>();

		dst.setNativePayment(src);
		dst.setCustomerPayment(true);
		dst.setPaymentDate(src.getPaymentDate());
		dst.setOriginalAmount(-src.getAmount());
		dst.setAmount(new Double(-src.getAmount()));
		dst.setOrderNo(src.getDescriptor());
		dst.setComment(src.getDescription());
		dst.setDestinationSystemReference(src.getTransactionId());
		dst.setDestinationSystemReferenceField("ExternalInvoiceReference2");
		dst.setDestinationSystemReferenceRegex("^pm_shopware:ratepay_invoice\\.(.*)$");
		// dst.setDestinationSystemReference(src.getShopsOrderId());
		// dst.setDestinationSystemReferenceField("order");
		dst.setClientOrderNo(src.getShopsOrderId());
		if (report.getCurrency()!=null) {
			dst.setCurrency(report.getCurrency());
		}

		TransactionReference trxRef = new TransactionReference();
		dst.setTransactionReference(trxRef);
		
		trxRef.setShipDate(LocalDateUtils.asLocalDate(src.getSentDate()));
		trxRef.setOrderDate(LocalDateUtils.asLocalDate(src.getOrderDate()));
		trxRef.setTransactionId(src.getTransactionId());
		if (src.getFees()!=null && src.getFees().size()>0) {
			for (RatepayFee fee : src.getFees()) {
				addFeeToPayment(dst, fee);
			}
		}
		
		dst.calculateAmountDeductingWriteOffsFromOriginalAmount();
		
		return dst;
		
	}
	
	private void addFeeToPayment(Payment<RatepayReportRow> payment, RatepayFee fee) {
		
		PaymentWriteOff pwo = new PaymentWriteOff();
		pwo.setAccountNo(fee.getFeeType().toString());
		pwo.setAmount(fee.getAmount());
		pwo.setComment(fee.getComment());
		payment.addPaymentWriteOff(pwo);
		
	}
	
	
	
}
