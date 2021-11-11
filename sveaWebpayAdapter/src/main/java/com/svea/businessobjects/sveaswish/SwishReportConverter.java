package com.svea.businessobjects.sveaswish;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.svea.webpay.common.conv.JsonUtil;
import com.svea.webpay.common.reconciliation.PaymentReport;
import com.svea.webpay.common.reconciliation.PaymentReportDetail;
import com.svea.webpay.common.reconciliation.PaymentReportGroup;

import org.notima.swish.reports.SettlementReport;
import org.notima.swish.reports.SettlementReportParser;
import org.notima.swish.reports.SettlementReportRow;

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
        group.setPaymentType(PAYMENT_TYPE_NAME);
        List<PaymentReportDetail> rows = convertRows(swishReport.getRows(), fromDate, toDate);
        group.setPaymentReportDetail(rows);
        paymentReport.addPaymentReportGroup(group);
        return paymentReport;
    }

    private List<PaymentReportDetail> convertRows(List<SettlementReportRow> rows, Date fromDate, Date toDate) {
        List<PaymentReportDetail> details = new ArrayList<PaymentReportDetail>();
        for(SettlementReportRow row : rows) {
            long rowTimeStamp = row.getTransactionDate().getTime();
            if(rowTimeStamp >= getStartOfDay(fromDate).getTime() && rowTimeStamp <= getEndOfDay(toDate).getTime()){
                details.add(convertToPaymentReportDetail(row));
            }
        }
        return details;
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

    private Date getStartOfDay(Date date) {
        Date result = new Date(date.getTime());
        result.setHours(0);
        result.setMinutes(0);
        result.setSeconds(0);
        return result;
    }
    
    private Date getEndOfDay(Date date) {
        Date result = new Date(date.getTime());
        result.setHours(23);
        result.setMinutes(59);
        result.setSeconds(59);
        return result;
    }
}
