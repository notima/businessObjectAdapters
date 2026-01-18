package org.notima.fortnox.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.entities3.InvoiceInterface;
import org.notima.api.fortnox.entities3.SupplierInvoice;
import org.notima.api.fortnox.entities3.SupplierInvoiceSubset;
import org.notima.api.fortnox.entities3.SupplierInvoices;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.ReportFormatter;
import org.notima.businessobjects.adapter.tools.command._NotimaCmdOptions;
import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.SupplierInvoiceHeaderTable;

@Command(scope = _FortnoxCommandNames.SCOPE, name = _FortnoxCommandNames.ListSupplierInvoices, description = "Lists supplier invoices in Fortnox")
@Service
public class ListSupplierInvoices extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Reference
	private FormatterFactory	formatterFactory;
	
	private ReportFormatter<GenericTable> reportFormatter;
	
    @Option(name = _NotimaCmdOptions.OUTPUT_FILE_NAME_SHORT, description="Output to file name", required = false, multiValued = false)
    private String	outFile;
    
    @Option(name = _NotimaCmdOptions.FORMAT_SHORT, description="The format of file to be output", required = false, multiValued = false)
    private String format;
	
	@Option(name = "-e", aliases = {
	"--enrich" }, description = "Read the complete invoice, not just the subset", required = false, multiValued = false)
	private boolean enrich;

	@Option(name = "--all", description = "Show all invoices", required = false, multiValued = false)
	private boolean all;
	
	@Option(name = _FortnoxOptions.FromDate, description = "Select invoices from this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDateStr;
	
	@Option(name = _FortnoxOptions.UtilDate, description = "Select invoices until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String untilDateStr;
	
	@Option(name = "--unbooked", description = "Show unbooked invoices", required = false, multiValued = false)
	private boolean unbooked;
	
	@Option(name = "--show-cancelled", description = "Show cancelled invoices", required = false, multiValued = false)
	private boolean showCancelled = false;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)
	private String orgNo = "";

	private Map<Object, Object> invoicesMap = null;
	private List<InvoiceInterface> invoices; 
	
	private GenericTable printableReport;
	
	
	@Override
	public Object execute() throws Exception {

		this.getBusinessObjectFactoryForOrgNo(orgNo);

		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}

		parseDates(fromDateStr, untilDateStr);
		
		invoices = new ArrayList<InvoiceInterface>();
		
		if (!all) {
			if (unbooked) {
				invoicesMap = bf.lookupList(FortnoxAdapter.LIST_UNPOSTED, false);
			} else {
				invoicesMap = bf.lookupList(FortnoxAdapter.LIST_UNPAID, false);
			}
		} else {
			
			FortnoxClient3 fc = getFortnoxClient(orgNo);
			
			SupplierInvoices allInvoices = fc.getAllSupplierInvoicesByDateRange(fromDate, untilDate);
			if (allInvoices.getSupplierInvoiceSubset()!=null) {
				invoices.addAll(allInvoices.getSupplierInvoiceSubset());
			}
			
		}
		
		checkCancelledAndDateRange();
		
		if (invoices.size()>0) {

			printableReport = new SupplierInvoiceHeaderTable(invoices);
			printableReport.getShellTable().print(sess.getConsole());
			
			sess.getConsole().println("\n" + invoices.size() + " invoice(s).");
			
		}
		
		initAndRunReportFormatter();
		
		return null;
	}
	
	private void checkCancelledAndDateRange() throws Exception {
		
		if (invoicesMap!=null) {
			
			Collection<InvoiceInterface> invoiceObjects = new ArrayList<InvoiceInterface>();
			
			for (Object o :	invoicesMap.values()) {
				if (o instanceof InvoiceInterface) {
					invoiceObjects.add((InvoiceInterface)o);
				}
			}
			
			SupplierInvoice inv = null;
			SupplierInvoiceSubset invs = null;
			for (InvoiceInterface oo : invoiceObjects) {
				
				// Check date filter
				if (!FortnoxUtil.isInDateRange(oo.getInvoiceDate(), fromDate, untilDate))
					continue;
				
				if (oo instanceof SupplierInvoice) {
					inv = (SupplierInvoice)oo;
					if (!inv.isCancelled() || showCancelled) {
						invoices.add(oo);
					}
				}
				if (oo instanceof SupplierInvoiceSubset) {
					invs = (SupplierInvoiceSubset)oo;
					if (!invs.isCancelled() || showCancelled) {
						invoices.add(oo);
					}
				}
				
			}
			
		}
	
	}

	
	@SuppressWarnings("unchecked")
	private void initAndRunReportFormatter() throws Exception {

		if (format!=null) {
			
			// Try to find a report formatter
			reportFormatter = (ReportFormatter<GenericTable>) formatterFactory.getReportFormatter(GenericTable.class, format);
			
			if (reportFormatter!=null) {
				Properties props = new Properties();
				props.setProperty(BasicReportFormatter.OUTPUT_FILENAME, outFile);
				
				String of = reportFormatter.formatReport((GenericTable)printableReport, format, props);
				sess.getConsole().println("Output file to: " + of);
			} else {
				sess.getConsole().println("Can't find formatter for " + format);
			}
			
		}
		
	}
	
}
