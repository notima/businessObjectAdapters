package org.notima.businessobjects.adapter.tools.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.table.BalanceSheetTable;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.BalanceSheetReport;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.util.LocalDateUtils;
import org.notima.generic.ifacebusinessobjects.AccountingReportProvider;

@Command(scope = "notima", name = "show-balance-report", description = "Shows Balance Sheet Report")
@Service
public class ShowBalanceReport implements Action {
	
	public static DateFormat	s_dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
	@Reference
	private List<AccountingReportProvider> arps;
	
	@Reference
	private Session sess;
	
	@Option(name = "--fromdate", description = "Limit from this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDateStr;
	
	@Option(name = "--untildate", description = "Limit until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String untilDateStr;
	
    @Argument(index = 0, name = "orgNo", description = "The org number to show report for", required = true, multiValued = false)
    private String orgNo;
	
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
		
		BalanceSheetReport report = arp.getBalanceSheet(bp, ap, null, null);
		BalanceSheetTable plt = new BalanceSheetTable(report);
		plt.getShellTable().print(sess.getConsole());
		
		return null;
	}
	
}
