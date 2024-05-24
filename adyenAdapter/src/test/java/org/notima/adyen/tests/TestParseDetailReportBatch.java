package org.notima.adyen.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.notima.adyen.AdyenReport;
import org.notima.adyen.AdyenReportParser;

public class TestParseDetailReportBatch {

	private File fileToTest;
	private AdyenReport report;
	
	@Before
	public void setUp() throws Exception {
		fileToTest = TestUtil.getFileFromResources("settlement_detail_report_batch_93.xlsx");
	}

	@Test
	public void test() throws IOException, Exception {
		
		report = AdyenReportParser.createFromFile(fileToTest.getCanonicalPath());
		
		if (report==null) {
			fail("Not yet implemented");
		} else {
			
			System.out.println("Settlement date: " + report.getSettlementDate());
			
		}
		
	}

}
