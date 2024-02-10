package org.notima.businessobjects.adapter.csv;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CsvOrderToFile {

	private File	outFile;
	
	public File getOutFile() {
		return outFile;
	}

	public void setOutFile(File outFile) {
		this.outFile = outFile;
	}

	public void persistOrdersToFile(List<Object[]> nativeOrders, String file) throws Exception {

		StringBuffer buf = new StringBuffer();
		
		CSVPrinter printer = new CSVPrinter(buf, CSVFormat.EXCEL);
		
		// Print headers
		printer.printRecord((Object[])CsvConverter.headers);
		
		for (Object[] r : nativeOrders) {
			
			printer.printRecord(r);
			
		}
		
		printer.close();
		
		if (outFile!=null) {
			
			if (!outFile.getAbsolutePath().toLowerCase().endsWith(".csv")) {
				outFile = new File(outFile.getAbsolutePath() + ".csv");
			}
			
			PrintWriter fp = new PrintWriter(outFile);
			fp.append(buf.toString());
			fp.append("\n");
			fp.close();
			
		}
		
		
	}
	
}
