package org.notima.businessobjects.adapter.tools.command;

import java.io.FileInputStream;
import java.io.FileReader;
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
import org.notima.generic.businessobjects.OrderInvoiceWriterOptions;
import org.notima.generic.businessobjects.tools.InvoiceListMerger;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.util.LocalDateUtils;
import org.notima.util.json.JsonUtil;

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

    @Option(name="--merge-on-customer", description="Merge invoices if there are many on the same customer", required = false, multiValued = false)
    private boolean mergeOnBp;
    
    @Option(name="--use-tax-id", description="Use tax ID to map the customer", required = false, multiValued = false)
    private boolean useTaxId;

    @Option(name="--map-on-address-first", description="Use address first to map the customer", required = false, multiValued = false)
    private boolean mapOnAddressFirst;
    
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
	
	private OrderInvoiceWriterOptions	writerOptions = new OrderInvoiceWriterOptions();
	
	@Override
	protected Object onExecute() throws Exception {
		
		initBusinessObjectFactory();
		parseOptions();
		parseInvoiceFile();
		checkUseTaxId();
		mergeIfThatsAnOption();
		writeInvoices();
		
		return null;
	}
	
	private void initBusinessObjectFactory() throws Exception {
		adapter = cof.lookupAdapter(adapterName);
		
		adapter.setTenant(orgNo, countryCode);
		
	}

	private void mergeIfThatsAnOption() {
		if (mergeOnBp) {
			writerOptions.setMergeOnBusinessPartner(mergeOnBp);
			InvoiceListMerger merger = new InvoiceListMerger(invoiceList);
			merger.mergeListByBusinessPartner();
			invoiceList = merger.getList();
			invoices.clear();
			invoices.addAll(invoiceList.getInvoiceList());
		}
	}
	
	private void parseOptions() throws ParseException {
		
		if (invoiceDateStr!=null) {
			invoiceDate = dfmt.parse(invoiceDateStr);
			writerOptions.setInvoiceDate(LocalDateUtils.asLocalDate(invoiceDate));
		}
		if (dueDateStr!=null) {
			dueDate = dfmt.parse(dueDateStr);
			writerOptions.setDueDate(LocalDateUtils.asLocalDate(dueDate));
		}
		
		if (createLimit==null) createLimit = 0;
		writerOptions.setCreateLimit(createLimit);
		writerOptions.setMapOnTaxId(useTaxId);
		writerOptions.setMapOnAddressFirst(mapOnAddressFirst);
		
	}
	
	
	private void writeInvoices() throws Exception {
		
		adapter.writeInvoices(invoices, writerOptions);
		
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

		if (invoiceFile.toLowerCase().endsWith("xml")) {
			parseInvoiceXMLFile();
		}
		if (invoiceFile.toLowerCase().endsWith("json")) {
			parseInvoiceJsonFile();
		}
		
	}
	
	private void parseInvoiceJsonFile() throws IOException {
		
		FileReader fis = new FileReader(invoiceFile);
		invoiceList = JsonUtil.buildGson().fromJson(fis, InvoiceList.class);
		fis.close();
		invoices = invoiceList.getInvoiceList();
		
	}
	
	private void parseInvoiceXMLFile() throws IOException {
		
		FileInputStream fis = new FileInputStream(invoiceFile);
		invoiceList = JAXB.unmarshal(fis, InvoiceList.class);
		fis.close();
		invoices = invoiceList.getInvoiceList();
		
	}
	
	
}
