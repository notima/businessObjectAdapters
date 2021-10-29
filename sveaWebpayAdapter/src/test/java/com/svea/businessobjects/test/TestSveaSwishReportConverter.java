package com.svea.businessobjects.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.svea.businessobjects.sveaswish.SwishReportConverter;
import com.svea.webpay.common.reconciliation.PaymentReport;

import org.junit.Test;
import org.notima.swish.reports.SettlementReport;
import org.notima.swish.reports.SettlementReportRow;

public class TestSveaSwishReportConverter {
    
	@Test
	public void testSwishReportToPaymentReport() {
		SettlementReport report = new SettlementReport();
		report.setAccountNumber("123");
		report.setClearingNumber("321");
		List<SettlementReportRow> rows = new ArrayList<SettlementReportRow>();
		for(int i = 0; i < 20; i++) {
			SettlementReportRow row = new SettlementReportRow();
            row.setAmount(i * 1.93 + 100 * (i % 4 == 0 ? -1 : 1));
			row.setBookKeepingDate(new Date());
			row.setCurrencyDate(new Date());
			row.setTransactionDate(new Date());
			row.setMessage("Message " + i);
			if(i % 4 == 0){
				row.setRecipientName("Person " + i);
				row.setRecipientNumber("+467345678" + i + (i < 10 ? "0":""));
				row.setSenderName("Company");
				row.setSenderNumber("1234567890");
			} else {
				row.setSenderName("Person " + i);
				row.setSenderNumber("+467345678" + i + (i < 10 ? "0":""));
				row.setRecipientName("Company");
				row.setRecipientNumber("1234567890");
			}
			row.setOrderReference("123" + i + (i < 10 ? "0":""));
			rows.add(row);
		}
		report.setRows(rows);

		SwishReportConverter converter = new SwishReportConverter();
		PaymentReport pReport = converter.convert(report);
		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(pReport));

		assertEquals(pReport.getPaymentReportGroup().get(0).getPaymentReportDetail().get(1).getPayerName(), rows.get(1).getSenderName());
		assertEquals(pReport.getPaymentReportGroup().get(0).getPaymentReportDetail().get(4).getPayerName(), rows.get(4).getRecipientName());
	}
}
