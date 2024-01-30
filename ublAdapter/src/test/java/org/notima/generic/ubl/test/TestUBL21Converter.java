package org.notima.generic.ubl.test;

import static org.junit.Assert.fail;

import java.io.File;

import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

import org.junit.Test;
import org.notima.generic.ubl.factory.UBL21Converter;

import com.helger.ubl21.UBL21Writer;

public class TestUBL21Converter {

	@Test
	public void testConvert() {
		
		try {
			TestConfig.loadConfig();
			
			if (TestConfig.sampleBoInvoice!=null) {
	
				InvoiceType result = UBL21Converter.convert(TestConfig.sampleBoInvoice);
				result = UBL21Converter.addPaymentMeansBankgiro(result, "51234567", result.getIDValue(), "The Fictive Company AB");
				
				UBL21Writer.invoice().write(result, new File("target/test-invoice.xml"));
				
			}
		} catch (Exception ee) {
			fail(ee.getMessage());
			ee.printStackTrace();
		}
		
	}

}
