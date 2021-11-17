package org.notima.businessobjects.adapter.ratepay;

import java.util.List;

import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TransactionReference;
import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportRow;
import org.notima.util.LocalDateUtils;

/**
 * Converts a Ratepay file to a payment batch.
 * 
 * @author Daniel Tamm
 *
 */
public class RatepayToPaymentBatch {

	private List<RatepayReportRow> rows;
	private PaymentBatch batch;
	
	public static RatepayToPaymentBatch buildFromReport(RatepayReport report) {
		
		RatepayToPaymentBatch ratepay = new RatepayToPaymentBatch();
		ratepay.rows = report.getReportRows();
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
		dst.setPaymentDate(src.getPaymentDate());
		dst.setAmount(src.getAmount());
		dst.setOrderNo(src.getShopsOrderId());
		dst.setComment(src.getDescription());

		TransactionReference trxRef = new TransactionReference();
		dst.setTransactionReference(trxRef);
		
		trxRef.setShipDate(LocalDateUtils.asLocalDate(src.getSentDate()));
		trxRef.setTransactionId(src.getTransactionId());
		
		return dst;
		
	}
	
	
}
