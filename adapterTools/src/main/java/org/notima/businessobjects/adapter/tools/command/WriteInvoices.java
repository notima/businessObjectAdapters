package org.notima.businessobjects.adapter.tools.command;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "write-invoices", description = "Reads invoices from an XML-file and writes them to the destination adapter")
@Service
public class WriteInvoices extends AbstractAction {
	
	@Reference
	private CanonicalObjectFactory cof;

    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;
	
    @Option(name="--invoice-date", description="Invoice date (defaults to today)", required = false, multiValued = false)
    private String	invoiceDateStr;

    @Option(name="--due-date", description="Due date (defaults +10 days from invoice date)", required = false, multiValued = false)
    private String	dueDateStr;

    @Option(name="--create-limit", description="Create limit.", required = false, multiValued = false)
    private Integer	createLimit;
    
    @Option(name="--use-tax-id", description="Use tax ID to map the customer", required = false, multiValued = false)
    private boolean useTaxId;
    
	@Argument(index = 0, name = "adapterName", description ="The destination adapter name", required = true, multiValued = false)
	private String adapterName = "";

    @Argument(index = 1, name = "orgNo", description = "The org number of the tenant to write to", required = true, multiValued = false)
    private String orgNo;
	
	@Argument(index = 2, name = "invoiceFile", description ="The canonical invoice file (xml-format)", required = true, multiValued = false)
	@Completion(FileCompleter.class)   
	private String invoiceFile = "";
	
	private BusinessObjectFactory<?,?,?,?,?,?> adapter;
	private InvoiceList invoiceList = null;
	private List<Invoice<?>> invoices = new ArrayList<Invoice<?>>();
	
	private Date	invoiceDate;
	private Date	dueDate;
	
	@Override
	protected Object onExecute() throws Exception {
		
		initBusinessObjectFactory();
		parseOptions();
		parseInvoiceFile();
		checkUseTaxId();
		writeInvoices();
		
		return null;
	}
	
	private void initBusinessObjectFactory() throws Exception {
		adapter = cof.lookupAdapter(adapterName);
		
		adapter.setTenant(orgNo, countryCode);
		
	}

	private void parseOptions() throws ParseException {
		
		if (invoiceDateStr!=null) {
			invoiceDate = dfmt.parse(invoiceDateStr);
		}
		if (dueDateStr!=null) {
			dueDate = dfmt.parse(dueDateStr);
		}
		
		if (createLimit==null) createLimit = 0;
		
	}
	
	
	private void writeInvoices() throws Exception {
		
		adapter.writeInvoices(invoices, invoiceDate, dueDate, false, createLimit, true);
		
	}
	
	private void checkUseTaxId() {
		
		if (useTaxId) {
			
			for (Invoice<?> invoice : invoices) {
				if (invoice.getBusinessPartner().getIdentityNo()!=null && invoice.getBusinessPartner().hasTaxId()) {
					invoice.getBusinessPartner().setIdentityNo(null);
				}
			}
			
		}
		
	}
	
	
	private void parseInvoiceFile() throws IOException {
		
		FileInputStream fis = new FileInputStream(invoiceFile);
		invoiceList = JAXB.unmarshal(fis, InvoiceList.class);
		fis.close();
		invoices = invoiceList.getInvoiceList();
		
	}
	
	
}
