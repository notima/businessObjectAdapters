package org.notima.businessobjects.adapter.tools.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import org.notima.businessobjects.adapter.tools.table.AccountingVoucherListTable;
import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.AccountingReportProvider;
import org.notima.util.LocalDateUtils;

@Command(scope = "notima", name = "list-accounting-vouchers", description = "Shows a list of accounting vouchers in given series")
@Service
public class ListAccountingVouchers implements Action {
	
	public static DateFormat	s_dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
	@Reference
	private FormatterFactory	formatterFactory;
	
	@Reference
	private List<AccountingReportProvider> arps;
	
	@Reference
	private Session sess;
	
	@Option(name = "--fromdate", description = "Select vouchers from this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDateStr;
	
	@Option(name = "--untildate", description = "Select vouchers until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String untilDateStr;
	
    @Option(name="-of", description="Output result to file name", required = false, multiValued = false)
    private String	outFile;
    
    @Option(name="-format", description="The format of file to be output", required = false, multiValued = false)
    private String format;
	
    @Argument(index = 0, name = "orgNo", description = "The org number to show details for", required = true, multiValued = false)
    private String orgNo;
    
    @Argument(index = 1, name = "series", description = "The voucher series", required = true, multiValued = false)
    private String series;
	
	@Override
	public Object execute() throws Exception {

		// Iterate through the providers
		AccountingReportProvider arp = null;
		
		if (arps==null) {
			sess.getConsole().println("No accounting report providers registered.");
			return null;
		}
		
		BusinessPartner<?> bp = new BusinessPartner<Object>();
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
		}
		if (untilDateStr!=null) {
			untilDate = s_dfmt.parse(untilDateStr);
		}

		AccountingPeriod ap = new AccountingPeriod();
		ap.setPeriodStart(LocalDateUtils.asLocalDate(fromDate));
		ap.setPeriodEnd(LocalDateUtils.asLocalDate(untilDate));

		List<AccountingVoucher> vouchers = arp.getAccountingVoucherList(bp, ap, series);
		
		AccountingVoucherListTable ast = new AccountingVoucherListTable(vouchers, true);
		ast.getShellTable().print(sess.getConsole());
		
		if (format!=null) {
			
			// Try to find a report formatter
			@SuppressWarnings("unchecked")
			ReportFormatter<GenericTable> rf = (ReportFormatter<GenericTable>) formatterFactory.getReportFormatter(GenericTable.class, format);
			
			if (rf!=null) {
				Properties props = new Properties();
				props.setProperty(BasicReportFormatter.OUTPUT_FILENAME, outFile);

				// Don't format the numbers when exporting.
				ast = new AccountingVoucherListTable(vouchers, false);
				
				String of = rf.formatReport((GenericTable)ast, format, props);
				sess.getConsole().println("Output file to: " + of);
			} else {
				sess.getConsole().println("Can't find formatter for " + format);
			}
			
		}
		
		
		
		return null;
	}
	
}
