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
import org.notima.businessobjects.adapter.tools.table.AccountStatementTable;
import org.notima.generic.businessobjects.AccountStatementLines;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.util.LocalDateUtils;
import org.notima.generic.ifacebusinessobjects.AccountingReportProvider;

@Command(scope = "notima", name = "show-gl-details", description = "Shows GL-details")
@Service
public class ShowGLDetails implements Action {
	
	public static DateFormat	s_dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
	@Reference
	private List<AccountingReportProvider> arps;
	
	@Reference
	private Session sess;
	
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
		
		
		AccountStatementLines lines = arp.getAccountStatementLines(bp, accountNo, 
				LocalDateUtils.asLocalDate(fromDate), 
				LocalDateUtils.asLocalDate(untilDate), orderByAmount);
		
		AccountStatementTable ast = new AccountStatementTable(lines);
		ast.print(sess.getConsole());
		
		return null;
	}
	
}
