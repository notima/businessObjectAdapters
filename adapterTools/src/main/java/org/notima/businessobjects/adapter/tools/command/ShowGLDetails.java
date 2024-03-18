package org.notima.businessobjects.adapter.tools.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.ReportFormatter;
import org.notima.businessobjects.adapter.tools.table.AccountStatementTable;
import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.notima.generic.businessobjects.AccountStatementLines;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.util.LocalDateUtils;
import org.notima.generic.ifacebusinessobjects.AccountingReportProvider;

@Command(scope = "notima", name = "show-gl-details", description = "Shows GL-details")
@Service
public class ShowGLDetails implements Action {
	
	public static DateFormat	s_dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
	@Reference
	protected FormatterFactory	formatterFactory;
	
	protected ReportFormatter<GenericTable> reportFormatter;
	
    protected GenericTable printableReport;
	
	@Reference
	private List<AccountingReportProvider> arps;
	
	@Reference
	private Session sess;
	
    @Option(name = _NotimaCmdOptions.OUTPUT_FILE_NAME_SHORT, description="Output to file name", required = false, multiValued = false)
    private String	outFile;
    
    @Option(name = _NotimaCmdOptions.FORMAT_SHORT, description="The format of file to be output", required = false, multiValued = false)
    private String format;
	
	@Option(name = "--fromdate", description = "Select invoices from this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDateStr;
	
	@Option(name = "--untildate", description = "Select invoices until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String untilDateStr;
	
	@Option(name = "--orderbyamount", description = "Order transactions by amount (after date)", required = false, multiValued = false)
	private boolean orderByAmount;
	
    @Argument(index = 0, name = "orgNo", description = "The org number to show details for", required = true, multiValued = false)
    private String orgNo;
    
    @Argument(index = 1, name = "accountNo", description = "The account number to show details for", required = true, multiValued = false)
    private String accountNo;

    private AccountingReportProvider arp = null;

    private List<AccountStatementLines> allLines;
    
    private LocalDate fromDateLocal;
    private LocalDate untilDateLocal;

	private BusinessPartner<?> bp = new BusinessPartner<Object>();
    
	@Override
	public Object execute() throws Exception {

		// Iterate through the providers
		if (arps==null) {
			sess.getConsole().println("No accounting report providers registered.");
			return null;
		}
		
		bp.setTaxId(orgNo);
		
		for (AccountingReportProvider aa : arps) {
			if (aa.hasAccountingFor(bp)) {
				arp = aa;
				break;
			}
		}
		
		if (arp==null) {
			sess.getConsole().println("No report provider found for " + orgNo);
			return null;
		}

		Date fromDate = null, untilDate = null;
		
		if (fromDateStr!=null) {
			fromDate = s_dfmt.parse(fromDateStr);
			fromDateLocal = LocalDateUtils.asLocalDate(fromDate);
		}
		if (untilDateStr!=null) {
			untilDate = s_dfmt.parse(untilDateStr);
			untilDateLocal = LocalDateUtils.asLocalDate(untilDate);
		}
				
		addAccountStatementLinesForAccount();
		
		for (AccountStatementLines lines : allLines) {
			printReport(lines);
		}
		
		return null;
	}
	
	private void printReport(AccountStatementLines lines) throws Exception {
		
		AccountStatementTable ast = new AccountStatementTable(lines);
		ast.getShellTable().print(sess.getConsole());
		printableReport = ast;
		initAndRunReportFormatter();
		
	}
	
	private void addAccountStatementLinesForAccount() throws Exception {
		AccountStatementLines accountLines = 
				arp.getAccountStatementLines(bp, accountNo, 
						fromDateLocal, 
						untilDateLocal, orderByAmount);
		
		if (allLines==null) {
			allLines = new ArrayList<AccountStatementLines>();
		}
		allLines.add(accountLines);
		
	}
	
	@SuppressWarnings("unchecked")
	protected void initAndRunReportFormatter() throws Exception {

		if (format!=null && printableReport!=null) {
			
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
