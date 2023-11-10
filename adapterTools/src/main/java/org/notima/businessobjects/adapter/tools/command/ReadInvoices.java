package org.notima.businessobjects.adapter.tools.command;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.InvoiceOperationResult;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "read-invoices", description = "Reads invoices from an adapter and writes them to the destination adapter (or XML-file if no adapter is specified")
@Service
public class ReadInvoices extends AbstractAction {
	
	@Reference
	private CanonicalObjectFactory cof;

    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;
	
    @Option(name="--from-date", description="From date", required = false, multiValued = false)
    private String	fromDateStr;

    @Option(name="--until-date", description="Until date", required = false, multiValued = false)
    private String	untilDateStr;

    @Option(name="--create-limit", description="Create limit.", required = false, multiValued = false)
    private Integer	createLimit;
    
    
	@Argument(index = 0, name = "adapterName", description ="The source adapter name", required = true, multiValued = false)
	private String adapterName = "";

    @Argument(index = 1, name = "orgNo", description = "The org number of the tenant to read from", required = true, multiValued = false)
    private String orgNo;
	
	@Argument(index = 2, name = "invoiceFile", description ="The canonical invoice to write to (xml-format)", required = true, multiValued = false)
	@Completion(FileCompleter.class)   
	private String invoiceFile = "";
	
	private BusinessObjectFactory<?,?,?,?,?,?> adapter;
	private InvoiceOperationResult invoiceResult;
	
	private Date	fromDate;
	private Date	untilDate;
	
	@Override
	protected Object onExecute() throws Exception {
		
		initBusinessObjectFactory();
		parseOptions();
		readInvoices();
		writeInvoicesToXmlFile();
		
		return null;
	}
	
	private void initBusinessObjectFactory() throws Exception {
		adapter = cof.lookupAdapter(adapterName);
		
		adapter.setTenant(orgNo, countryCode);
		
	}

	private void writeInvoicesToXmlFile() throws IOException {
		
		FileOutputStream fis = new FileOutputStream(invoiceFile);
		JAXB.marshal(invoiceResult.getAffectedInvoices(), fis);
		fis.close();
		
	}
	
	private void parseOptions() throws ParseException {
		
		if (fromDateStr!=null) {
			fromDate = dfmt.parse(fromDateStr);
		}
		if (untilDateStr!=null) {
			untilDate = dfmt.parse(untilDateStr);
		}
		
		if (createLimit==null) createLimit = 0;
		
	}
	
	
	private void readInvoices() throws Exception {
		
		invoiceResult = adapter.readInvoices(fromDate, untilDate, createLimit);
		
	}
	
	
}
