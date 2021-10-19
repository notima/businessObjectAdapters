package org.notima.ratepay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.utils.InputStreamReader;

public class RatepayReportParser {

    private static final String K_SHOP_ID = "SHOP_ID";
    private static final String K_PAYMENTDATE = "PAYMENTDATE";
    private static final String K_SHOPNAME = "SHOPNAME";
    private static final String K_AMOUNT = "AMOUNT";
    private static final String K_DESCRIPTOR = "DESCRIPTOR";
    private static final String K_SHOPINVOICE_ID = "SHOPINVOICE_ID";
    private static final String K_SHOPSORDER_ID = "SHOPSORDER_ID";
    private static final String K_INVOICENUMBER = "INVOICENUMBER";
    private static final String K_DESCRIPTION = "DESCRIPTION";
    private static final String K_FEETYPE = "FEETYPE";
    private static final String K_ORDERDATE = "ORDERDATE";
    private static final String K_SENTDATE = "SENTDATE";
    private static final String K_TRANSACTION_ID = "TRANSACTION_ID";
    private static final String K_CUSTOMERGROUP = "CUSTOMERGROUP";
    private static final String K_KNOWNCUSTOMER = "KNOWNCUSTOMER";
    private static final String K_PRODUCT = "PRODUCT";
    private static final String K_REFERENCE_ID_ACCOUNTING = "REFERENCE_ID_ACCOUNTING";

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public List<RatepayReportRow> parseFile (InputStream inStream) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        List<RatepayReportRow> report = new ArrayList<RatepayReportRow>();
        Map<Integer, String> indexMap = getHeaderIndicies(reader.readLine().replaceAll("\"", ""));
        String line = reader.readLine();
        while(line != null) {
            RatepayReportRow row = parseLine(line.replaceAll("\"", ""), indexMap);
            report.add(row);
            line = reader.readLine();
        }
        reader.close();
        inStream.close();
        return report;
    }

    private Map<Integer, String> getHeaderIndicies(String headerLine) {
        Map<Integer, String> indexMap = new HashMap<Integer, String>();
        String[] headers = headerLine.split(";");
        for(int i = 0; i < headers.length; i++) {
            indexMap.put(i, headers[i]);
        }
        return indexMap;
    }

    private RatepayReportRow parseLine(String line, Map<Integer, String> indexMap) throws ParseException {
        RatepayReportRow row = new RatepayReportRow();
        String[] values = line.split(";");
        for(int i = 0 ; i < values.length; i++) {
            System.out.println(indexMap.get(i));
            System.out.println(values[i]);
            if(indexMap.get(i).equals(K_SHOP_ID)){
                System.out.println("SHOPID!");
                row.setShopId(values[i]);
            }
            if(indexMap.get(i).equals(K_PAYMENTDATE))
                row.setPaymentDate(dateFormat.parse(values[i]));
            if(indexMap.get(i).equals(K_SHOPNAME))
                row.setShopName(values[i]);
            if(indexMap.get(i).equals(K_AMOUNT))
                row.setAmount(Double.parseDouble(values[i]));
            if(indexMap.get(i).equals(K_DESCRIPTOR))
                row.setDescriptor(values[i]);
            if(indexMap.get(i).equals(K_SHOPINVOICE_ID))
                row.setShopInvoiceId(values[i]);
            if(indexMap.get(i).equals(K_SHOPSORDER_ID))
                row.setShopsOrderId(values[i]);
            if(indexMap.get(i).equals(K_INVOICENUMBER))
                row.setInvoiceNumber(values[i]);
            if(indexMap.get(i).equals(K_DESCRIPTION))
                row.setDescriptor(values[i]);
            if(indexMap.get(i).equals(K_FEETYPE))
                row.setFeeType(Integer.parseInt(values[i]));
            if(indexMap.get(i).equals(K_ORDERDATE))
                row.setOrderDate(dateFormat.parse(values[i]));
            if(indexMap.get(i).equals(K_SENTDATE))
                row.setSentDate(dateFormat.parse(values[i]));
            if(indexMap.get(i).equals(K_TRANSACTION_ID))
                row.setTransactionId(values[i]);
            if(indexMap.get(i).equals(K_CUSTOMERGROUP))
                row.setCustomerGroup(Integer.parseInt(values[i]));
            if(indexMap.get(i).equals(K_KNOWNCUSTOMER))
                row.setKnownCustomer(Integer.parseInt(values[i]) == 1);
            if(indexMap.get(i).equals(K_PRODUCT))
                row.setProduct(Integer.parseInt(values[i]));
            if(indexMap.get(i).equals(K_REFERENCE_ID_ACCOUNTING))
                row.setReferenceIdAccounting(Integer.parseInt(values[i]));
        }
        return row;
    }
}
