package org.notima.businessobjects.adapter.ratepay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.notima.ratepay.RatepayReportParser;
import org.notima.ratepay.RatepayReportRow;

public class TestRatepayUtil {

	public static List<RatepayReportRow> getTestReport() throws FileNotFoundException, IOException, ParseException {
		
        File file = new File("src/test/resources/test_settlement_report.csv");
        RatepayReportParser parser = new RatepayReportParser();
        List<RatepayReportRow> report = parser.parseFile(new FileInputStream(file));
        return report;
		
	}
	
}
