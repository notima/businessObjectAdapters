package org.notima.businessobjects.adapter.csv;

import java.io.FileReader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.AccountingVoucherConverter;
import org.notima.util.LocalDateUtils;

public class CsvToVouchers implements AccountingVoucherConverter<String> {

	public static DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd");
	
	public static String[] headers = new String[] {
		"acctDate",
		"crdAcct",
		"dbtAcct",
		"description",
		"amount"
	};
	
	private BusinessPartner<?> tenant;
	
	private List<AccountingVoucher> vouchers;
	
	private Date acctDate;
	private String crdAcct;
	private String dbtAcct;
	private String description;
	private double amount;
	
	private AccountingVoucher voucher;
	
	@Override
	public void readSource(String csvFilePath) {
		
		vouchers = new ArrayList<AccountingVoucher>();
		
        try (FileReader reader = new FileReader(csvFilePath);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(';').withHeader(headers).withSkipHeaderRecord())) {

               for (CSVRecord record : csvParser) {
                   acctDate = parseDate(record.get("acctDate"));
                   crdAcct = record.get("crdAcct");
                   dbtAcct = record.get("dbtAcct");
                   description = record.get("description");
                   amount = Double.parseDouble(record.get("amount"));

                   addVoucher();
                   
               }

           } catch (Exception e) {
               e.printStackTrace();
           }		
		
	}

	private void addVoucher() {
		voucher = new AccountingVoucher();
		voucher.setAcctDate(LocalDateUtils.asLocalDate(acctDate));
		voucher.setDescription(description);
		voucher.addVoucherLines(BigDecimal.valueOf(amount), dbtAcct, crdAcct);
		vouchers.add(voucher);
	}
	
	private static Date parseDate(String dateStr) throws ParseException {
        return dfmt.parse(dateStr);
    }	
	
	@Override
	public void setTenant(BusinessPartner<?> tenant) {
		this.tenant = tenant;
	}
	
	@Override
	public BusinessPartner<?> getTenant() {
		return tenant;
	}

	@Override
	public List<AccountingVoucher> getAccountingVouchers() {
		return vouchers;
	}

	@Override
	public String getSystemName() {
		return CsvAdapter.SYSTEM_NAME;
	}


}
