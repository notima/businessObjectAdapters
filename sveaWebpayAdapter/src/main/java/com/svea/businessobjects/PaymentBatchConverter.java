package com.svea.businessobjects;

import java.time.LocalDate;

import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;

import com.svea.webpay.common.reconciliation.PaymentReport;
import com.svea.webpay.common.reconciliation.PaymentReportDetail;
import com.svea.webpay.common.reconciliation.PaymentReportGroup;

/**
 * Converts a PaymentBatch into the Svea specific Payment Report format.
 * 
 * @author Daniel Tamm
 *
 */
public class PaymentBatchConverter {

	public PaymentReport convert(PaymentBatch canonicalReport, LocalDate fromDate, LocalDate toDate) {

		PaymentReport paymentReport = new PaymentReport();

		PaymentReportGroup group = new PaymentReportGroup();
		// Set currency from bank account
		group.setCurrency(canonicalReport.getBankAccount().getCurrency());
		paymentReport.addPaymentReportGroup(group);
		group.setPaymentType(canonicalReport.getSource());
		group.setPaymentTypeReference(canonicalReport.getBankAccount().getAccountNo());
		
		if (canonicalReport.isEmpty())
			return paymentReport;
		
		for (Payment<?> payment : canonicalReport.getPayments()) {
			
			group.addDetail(convertToDetail(payment));
			
		}
		
		return paymentReport;
		
	}
	
	
	private PaymentReportDetail convertToDetail(Payment<?> payment) {

		PaymentReportDetail detail = new PaymentReportDetail();
		
		detail.setCustomerId(payment.getBusinessPartnerKey());
		detail.setClientOrderNo(payment.getClientOrderNo());
		detail.setPaidAmt(payment.getAmount());
		detail.setPayerName(payment.getPayerName());
		detail.setOrderId(payment.getOrderNo());
		detail.setInvoiceId(payment.getInvoiceNo());
		
		return detail;
		
	}
	
	
}
