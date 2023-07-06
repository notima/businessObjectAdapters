package com.svea.businessobjects.sveaswish;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.notima.swish.reports.SettlementReport;
import org.notima.swish.reports.SettlementReportParser;
import org.notima.swish.reports.SettlementReportRow;
import org.notima.util.LocalDateUtils;

import com.svea.webpay.common.conv.JsonUtil;
import com.svea.webpay.common.reconciliation.PaymentReport;
import com.svea.webpay.common.reconciliation.PaymentReportDetail;
import com.svea.webpay.common.reconciliation.PaymentReportGroup;

public class SwishReportConverter {

    private static final String PAYMENT_TYPE_NAME = "Swish";

    private Date fromDate;
    private Date toDate;
    
    private SettlementReport settlementReport;
    
    public PaymentReport convert(String sourceFile) throws IOException, ParseException {
        File reportFile = new File(sourceFile);
        FileInputStream reportInputStream = new FileInputStream(reportFile);
        settlementReport = new SettlementReportParser().parseFile(reportInputStream);
        return convertFromSettlementReport(settlementReport);
    }

    public PaymentReport convertFromFile(String sourceFile, Date fromDate, Date toDate) throws IOException, ParseException {
        File reportFile = new File(sourceFile);
        FileInputStream reportInputStream = new FileInputStream(reportFile);
        settlementReport = new SettlementReportParser().parseFile(reportInputStream);
        this.fromDate = fromDate;
        this.toDate = toDate;
        return convertTheSettlementReport();
    }

    public PaymentReport convertFromSettlementReport(SettlementReport settlementReport) {
        this.settlementReport = settlementReport;
        return convertTheSettlementReport();
    }

    private void determineDatesFromSettlementReport() {

    	if (fromDate==null)
    		fromDate = settlementReport.getFirstBookkeepingDate();
    	if (toDate==null)
    		toDate = settlementReport.getLastBookkeepingDate();
    	
    }
    
    private PaymentReport convertTheSettlementReport() {
    	
    	determineDatesFromSettlementReport();
    	
        PaymentReport paymentReport = new PaymentReport();
        PaymentReportGroup group = new PaymentReportGroup();
        group.setCurrency("SEK");
        group.setReconciliationDate(fromDate);
        // TODO: This must be done in another way
        if (settlementReport.getRows()!=null && settlementReport.getRows().size()>0) {
        	group.setPaymentTypeReference(settlementReport.getRows().get(0).getRecipientNumber());
        }
        group.setPaymentType(PAYMENT_TYPE_NAME);
        List<PaymentReportDetail> rows = convertRows(settlementReport.getRows());
        for (PaymentReportDetail detail : rows) {
        	group.addDetail(detail);
        }
        paymentReport.addPaymentReportGroup(group);
        return paymentReport;
    }

    private List<PaymentReportDetail> convertRows(List<SettlementReportRow> rows) {
        List<PaymentReportDetail> details = new ArrayList<PaymentReportDetail>();
        LocalDate from = LocalDateUtils.asLocalDate(fromDate);
        LocalDate until = LocalDateUtils.asLocalDate(toDate);
        LocalDate bookkeepingDate;
        for(SettlementReportRow row : rows) {
            bookkeepingDate = LocalDateUtils.asLocalDate(row.getBookKeepingDate());
            if(includeAfter(bookkeepingDate, from) && 
            	includeBefore(bookkeepingDate, until)) {
                details.add(convertToPaymentReportDetail(row));
            }
        }
        return details;
    }

    private boolean includeAfter(LocalDate actualDate, LocalDate afterDate) {
    	if (actualDate==null || afterDate==null) return true;
    	if (actualDate.isEqual(afterDate) || actualDate.isAfter(afterDate)) 
    		return true;
    	return false;
    }
    
    private boolean includeBefore(LocalDate actualDate, LocalDate beforeDate) {
    	if (actualDate==null || beforeDate==null) return true;
    	if (actualDate.isEqual(beforeDate) || actualDate.isBefore(beforeDate))
    		return true;
    	return false;
    	
    	
    }
    
    private PaymentReportDetail convertToPaymentReportDetail(SettlementReportRow row) {
        PaymentReportDetail detail = new PaymentReportDetail();
        detail.setCustomerId(row.getAmount() > 0 ? row.getSenderNumber() : row.getRecipientNumber());
        detail.setPayerName(row.getAmount() > 0 ? row.getSenderName() : row.getRecipientName());
        detail.setClientOrderNo(row.getOrderReference());
        detail.setOrderDate(JsonUtil.getDateFormat().format(row.getTransactionDate()));
        detail.setPaidAmt(row.getAmount());
        detail.setReceivedAmt(row.getAmount());
        detail.setCheckoutOrderId(row.getCheckoutOrderId());
        return detail;
    }

}
