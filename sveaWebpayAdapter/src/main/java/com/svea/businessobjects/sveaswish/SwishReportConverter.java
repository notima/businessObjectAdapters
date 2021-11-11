package com.svea.businessobjects.sveaswish;

import java.util.ArrayList;
import java.util.List;

import com.svea.webpay.common.conv.JsonUtil;
import com.svea.webpay.common.reconciliation.PaymentReport;
import com.svea.webpay.common.reconciliation.PaymentReportDetail;
import com.svea.webpay.common.reconciliation.PaymentReportGroup;

import org.notima.swish.reports.SettlementReport;
import org.notima.swish.reports.SettlementReportRow;

public class SwishReportConverter {

    private static final String PAYMENT_TYPE_NAME = "Swish";

    public PaymentReport convert(SettlementReport swishReport) {
        PaymentReport paymentReport = new PaymentReport();
        PaymentReportGroup group = new PaymentReportGroup();
        group.setPaymentType(PAYMENT_TYPE_NAME);
        List<PaymentReportDetail> rows = convertRows(swishReport.getRows());
        group.setPaymentReportDetail(rows);
        paymentReport.addPaymentReportGroup(group);
        return paymentReport;
    }

    private List<PaymentReportDetail> convertRows(List<SettlementReportRow> rows) {
        List<PaymentReportDetail> details = new ArrayList<PaymentReportDetail>();
        for(SettlementReportRow row : rows) {
            details.add(convertToPaymentReportDetail(row));
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
}
