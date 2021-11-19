package org.notima.businessobjects.adapter.ratepay;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestRatepayUtil {

	public static Gson createGson() {
		Gson gson = new GsonBuilder().setPrettyPrinting().
				create();
		return gson;
	}
	
	public static RatepayReport getTestReport() throws FileNotFoundException, IOException, ParseException {
		
        String filename = "src/test/resources/test_settlement_report.csv";
        RatepayReport report = RatepayReportParser.createFromFile(filename);
        return report;
		
	}
	
}
