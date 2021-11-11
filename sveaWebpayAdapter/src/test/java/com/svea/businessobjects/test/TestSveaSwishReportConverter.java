package com.svea.businessobjects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.svea.businessobjects.sveaswish.SwishReportConverter;
import com.svea.webpay.common.reconciliation.PaymentReport;
import com.svea.webpay.common.reconciliation.PaymentReportDetail;

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
			row.setCheckoutOrderId("123456789" + i);
			rows.add(row);
		}
		report.setRows(rows);

		SwishReportConverter converter = new SwishReportConverter();
		PaymentReport pReport = converter.convert(report);
		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(pReport));

		assertEquals(pReport.getPaymentReportGroup().get(0).getPaymentReportDetail().get(1).getPayerName(), rows.get(1).getSenderName());
		assertEquals(pReport.getPaymentReportGroup().get(0).getPaymentReportDetail().get(4).getPayerName(), rows.get(4).getRecipientName());
	}

	@Test
	public void testConvertFile() throws Exception {
		SwishReportConverter converter = new SwishReportConverter();
		PaymentReport paymentReport = converter.convert("src/test/resources/Swishrapport.csv");
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(paymentReport);
		System.out.println(json);
	}

	@Test
	public void testGetTransactionsBetween() throws Exception {
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		Date fromDate = fmt.parse("2021-07-27");
		Date toDate = fmt.parse("2021-09-14");
		SwishReportConverter converter = new SwishReportConverter();
		PaymentReport paymentReport = converter.convert("src/test/resources/Swishrapport.csv", fromDate, toDate);
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(paymentReport);
		System.out.println(json);
		List<PaymentReportDetail> details = paymentReport.getPaymentReportGroup().get(0).getPaymentReportDetail();
		assertTrue(details.get(0).getOrderDateAsDate().getTime() >= fromDate.getTime());
		assertTrue(details.get(details.size() - 1).getOrderDateAsDate().getTime() <= toDate.getTime() + 86400000);
	}
}
