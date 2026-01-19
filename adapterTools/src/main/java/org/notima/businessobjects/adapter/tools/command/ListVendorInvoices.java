package org.notima.businessobjects.adapter.tools.command;

import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.InvoiceListFormatter;
import org.notima.businessobjects.adapter.tools.MappingServiceFactory;
import org.notima.businessobjects.adapter.tools.table.InvoiceHeaderTable;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceReaderOptions;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.util.LocalDateUtils;

@Command(scope = "notima", name = "list-vendor-invoices", description = "Lists vendor invoices from an adapter.")
@Service
public class ListVendorInvoices extends AbstractAction {
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	private FormatterFactory	formatterFactory;
	
	@Reference
	private MappingServiceFactory mappingFactory;
	
	private InvoiceListFormatter ilf;
	
	@Reference
	private Session	sess;

    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;
	
    @Option(name="--fromdate", description="From date", required = false, multiValued = false)
    private String	fromDateStr;

    @Option(name="--untildate", description="Until date", required = false, multiValued = false)
    private String	untilDateStr;
    
    @Option(name="-format", description="The format of match result file to be output", required = false, multiValued = false)
    private String format;
    
    @Option(name="--dateformat", description="Customize date format in report", required = false, multiValued = false)
    private String dateFormat;
    
    @Option(name="-of", description="Output match result to file name", required = false, multiValued = false)
    private String	outFile;
    
	@Argument(index = 0, name = "adapterName", description ="The source adapter name", required = true, multiValued = false)
	private String adapterName = "";

    @Argument(index = 1, name = "orgNo", description = "The org number of the tenant to read from", required = true, multiValued = false)
    private String orgNo;
	
	private BusinessObjectFactory<?,?,?,?,?,?> adapter;
	private OrderInvoiceReaderOptions readerOptions;
	private OrderInvoiceOperationResult invoiceResult;
	private boolean	unpostedOnly = false;
	private boolean salesOnly = false;
	
	private Date	fromDate;
	private Date	untilDate;
	
	@Override
	protected Object onExecute() throws Exception {
		
		initBusinessObjectFactory();
		parseOptions();
		readInvoices();
		printInvoices();
		
		return null;
	}
	
	private void initBusinessObjectFactory() throws Exception {
		adapter = cof.lookupAdapter(adapterName);
		
		adapter.setTenant(orgNo, countryCode);
		
	}
	
	private void parseOptions() throws ParseException, NoSuchTenantException, Exception {

		readerOptions = new OrderInvoiceReaderOptions();

		if (fromDateStr!=null) {
			fromDate = dfmt.parse(fromDateStr);
			readerOptions.setFromDate(LocalDateUtils.asLocalDate(fromDate));
		}
		if (untilDateStr!=null) {
			untilDate = dfmt.parse(untilDateStr);
			readerOptions.setUntilDate(LocalDateUtils.asLocalDate(untilDate));
		}

		readerOptions.setSalesOnly(salesOnly);
		readerOptions.setVendorOnly(true);
		readerOptions.setUnpostedOnly(unpostedOnly);
		
	}
	
	private void readInvoices() throws Exception {
		
		invoiceResult = adapter.readVendorInvoices(readerOptions);
		
	}
	
	
	private void printInvoices() throws Exception {
		
		InvoiceHeaderTable table = new InvoiceHeaderTable(invoiceResult.getAffectedInvoices().getInvoiceList(), false);
		table.getShellTable().print(sess.getConsole());
		
		writeToFormat();
	}
	
	private void writeToFormat() throws Exception {
		
		if (format!=null && invoiceResult.getAffectedInvoices()!=null) {
			
			// Try to find an invoice list report formatter
			ilf = formatterFactory.getInvoiceListFormatter(format);
			
			if (ilf!=null) {
				Properties props = new Properties();
				constructOutFile();
				if (dateFormat!=null) {
					props.setProperty(BasicReportFormatter.DATE_FORMAT, dateFormat);
				}
				props.setProperty(BasicReportFormatter.OUTPUT_FILENAME, outFile);
				
				String of = ilf.formatInvoice(invoiceResult.getAffectedInvoices(), format, props);
				sess.getConsole().println("Output file to: " + of);
				// Reset outfile for another run
				outFile = null;
			} else {
				sess.getConsole().println("Can't find formatter for " + format);
			}
			
		}
		
	}
	
	private void constructOutFile() {
		if (format!=null && outFile==null && ilf!=null) {
			// We need to construct an outfile.
			String filePrefix = "vendorlist-";
			outFile = filePrefix = format;
		}
	}
	
	
	
	
}
