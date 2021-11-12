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

    public PaymentReport convert(String sourceFile) throws IOException, ParseException {
        File reportFile = new File(sourceFile);
        FileInputStream reportInputStream = new FileInputStream(reportFile);
        SettlementReport report = new SettlementReportParser().parseFile(reportInputStream);
        return convert(report);
    }

    public PaymentReport convert(String sourceFile, Date fromDate, Date toDate) throws IOException, ParseException {
        File reportFile = new File(sourceFile);
        FileInputStream reportInputStream = new FileInputStream(reportFile);
        SettlementReport report = new SettlementReportParser().parseFile(reportInputStream);
        return convert(report, fromDate, toDate);
    }

    public PaymentReport convert(SettlementReport swishReport) {
        Date fromDate = new Date(0);
        Date toDate = new Date(Long.MAX_VALUE);
        return convert(swishReport, fromDate, toDate);
    }

    public PaymentReport convert(SettlementReport swishReport, Date fromDate, Date toDate) {
        PaymentReport paymentReport = new PaymentReport();
        PaymentReportGroup group = new PaymentReportGroup();
        group.setCurrency("SEK");
        // TODO: The reconciliation date must be derived from the rows
        group.setReconciliationDate(fromDate);
        // TODO: This must be done in another way
        if (swishReport.getRows()!=null && swishReport.getRows().size()>0) {
        	group.setPaymentTypeReference(swishReport.getRows().get(0).getRecipientNumber());
        }
        group.setPaymentType(PAYMENT_TYPE_NAME);
        List<PaymentReportDetail> rows = convertRows(swishReport.getRows(), fromDate, toDate);
        for (PaymentReportDetail detail : rows) {
        	group.addDetail(detail);
        }
        paymentReport.addPaymentReportGroup(group);
        return paymentReport;
    }

    private List<PaymentReportDetail> convertRows(List<SettlementReportRow> rows, Date fromDate, Date toDate) {
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
