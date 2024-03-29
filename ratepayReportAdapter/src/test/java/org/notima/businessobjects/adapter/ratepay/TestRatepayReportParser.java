package org.notima.businessobjects.adapter.ratepay;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.junit.Test;
import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportRow;

public class TestRatepayReportParser {
    
    @Test
    public void testParseFile() throws FileNotFoundException, IOException, Exception{
    	RatepayReport report = TestRatepayUtil.getTestReport();
    	
    	List<RatepayReportRow> rows = report.getReportRows();
    	
        for(RatepayReportRow row : rows) {
            System.out.printf(
                "%s\t%s\t%s\t%.2f\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%d\t%s\t%d\t%d\t\n", 
                row.getShopId(),
                row.getPaymentDate() == null ? "" : row.getPaymentDate().toString(),
                row.getShopName(),
                row.getAmount(),
                row.getDescriptor(),
                row.getShopInvoiceId(),
                row.getShopsOrderId(),
                row.getInvoiceNumber(),
                row.getDescription(),
                row.getFeeType(),
                row.getOrderDate() == null ? "" : row.getOrderDate().toString(),
                row.getSentDate() == null ? "" : row.getSentDate().toString(),
                row.getTransactionId(),
                row.getCustomerGroup(),
                row.isKnownCustomer() ? "true" : "false",
                row.getProduct(),
                row.getReferenceIdAccounting()
            );
        }
        assertEquals("12345678", rows.get(0).getShopId());
        assertEquals("shop.com", rows.get(1).getShopName());
        assertEquals(-10617.00, rows.get(2).getAmount(), 0.0);
    }
}
