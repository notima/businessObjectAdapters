package org.notima.businessobjects.adapter.excel.command;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.excel.ExcelToInvoices;
import org.notima.businessobjects.adapter.excel.completer.OutputFormatCompleter;
import org.notima.generic.businessobjects.InvoiceList;

@Command(scope = "notima-excel", name = "convert-spreadsheet", description = "Converts a spreadsheet to business objects")
@Service
public class ConvertSpreadSheet implements Action {

	@Reference
	private Session sess;
	
	@Option(name = "-of", description = "Outfile. If unspecified and out.xml-file at the same location is used", required = false, multiValued = false)
	@Completion(FileCompleter.class)
	private String outFile;
	
	
	@Argument(index = 0, name = "destinationFormat", description ="The destination format (implicits source format)", required = true, multiValued = false)
	@Completion(OutputFormatCompleter.class)
	private String destFormat;

	@Argument(index = 1, name = "inputFile", description ="The input file to convert", required = false, multiValued = false)
	@Completion(FileCompleter.class)
	private String inputFile;
	
	@Argument(index = 2, name = "inputFiles", description ="Additional input file(s) to convert (or sheet numbers)", required = false, multiValued = true)
	@Completion(FileCompleter.class)
	private List<String> inputFiles;
	
	@Override
	public Object execute() throws Exception {


		ExcelToInvoices eti = new ExcelToInvoices();

		InvoiceList il = eti.createInvoiceListFromFile(inputFile, inputFiles);

		File of = null;
		if (outFile==null) {
		
			// Get path from file if of is not set.
			File file = new File(inputFile);
			File path = file.getParentFile();
			
			of = new File(path.getAbsolutePath() + File.separator + "out.xml");
			
		} else {
			
			of = new File(outFile);
			
		}
		FileWriter fileWriter = new FileWriter(of);
		
		JAXB.marshal(il, fileWriter);
		fileWriter.close();
		
		sess.getConsole().println(of.getAbsolutePath() + " written.");
		
		return null;
	}

}
