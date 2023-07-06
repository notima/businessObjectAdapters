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
import org.notima.businessobjects.adapter.tools.table.AccountingVoucherTable;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.util.LocalDateUtils;
import org.notima.generic.ifacebusinessobjects.AccountingReportProvider;

@Command(scope = "notima", name = "show-accounting-voucher", description = "Shows an accounting voucher")
@Service
public class ShowAccountingVoucher implements Action {
	
	public static DateFormat	s_dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
	@Reference
	private List<AccountingReportProvider> arps;
	
	@Reference
	private Session sess;
	
	@Option(name = "--fromdate", description = "Select invoices from this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDateStr;
	
	@Option(name = "--untildate", description = "Select invoices until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String untilDateStr;
	
    @Argument(index = 0, name = "orgNo", description = "The org number to show details for", required = true, multiValued = false)
    private String orgNo;
    
    @Argument(index = 1, name = "series", description = "The voucher series", required = true, multiValued = false)
    private String series;
    
    @Argument(index = 2, name = "voucherNo", description = "The voucher number", required = true, multiValued = false)
    private String voucherNo;
	
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

		AccountingVoucher voucher = arp.getAccountingVoucher(bp, ap, series, voucherNo);
		
		AccountingVoucherTable ast = new AccountingVoucherTable(voucher);
		ast.getShellTable().print(sess.getConsole());
		
		return null;
	}
	
}
