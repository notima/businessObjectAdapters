package org.notima.fortnox.command;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.VoucherSubset;
import org.notima.api.fortnox.entities3.Vouchers;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.VouchersTable;

@Command(scope = _FortnoxCommandNames.SCOPE, name = _FortnoxCommandNames.ListVouchers, description = "Lists vouchers in Fortnox")
@Service
public class ListVouchers extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Option(name = "--yearId", description = "Vouchers for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Option(name = _FortnoxOptions.FromDate, description = "Select vouchers from this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDateStr;
	
	@Option(name = _FortnoxOptions.UtilDate, description = "Select vouchers until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String untilDateStr;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";
	
	@Argument(index = 1, name = "series", description ="The series", required = true, multiValued = false)
	private String series = "";
	
	private Vouchers vouchers;
	
	@Override
	public Object execute() throws Exception {

		bf = this.getBusinessObjectFactoryForOrgNo(orgNo);

		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}
		
		parseDates(fromDateStr, untilDateStr);		
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		
		vouchers = fc.getVouchers(yearId, series);
		filterVouchers();
		
		VouchersTable iht = new VouchersTable(vouchers);
		iht.print(sess.getConsole());
			
		sess.getConsole().println("\n" + vouchers.getTotalResources() + " voucher(s).");
		
		return null;
	}
	
	
	private void filterVouchers() {
		
		Vouchers filtered = new Vouchers();
		List<VoucherSubset> filteredList = new ArrayList<VoucherSubset>();
		filtered.setVoucherSubset(filteredList);
		
		if (vouchers==null || vouchers.getVoucherSubset()==null) {
			vouchers = filtered;
			return;
		}

		Date transactionDate;
		for (VoucherSubset vs : vouchers.getVoucherSubset()) {
			try {
				transactionDate = FortnoxClient3.getAsDate(vs.getTransactionDate());
				if (isInRange(transactionDate)) {
					filteredList.add(vs);
				}
			} catch (ParseException pe) {
				
			}
		}
		vouchers = filtered;
		
	}
		
}
