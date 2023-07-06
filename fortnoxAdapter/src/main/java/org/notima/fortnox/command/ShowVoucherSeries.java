package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.VoucherSeriesCollection;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.VoucherSeriesTable;

@Command(scope = "fortnox", name = "show-fortnox-voucher-series", description = "List voucher series")
@Service
public class ShowVoucherSeries extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Option(name = "--yearId", description = "Config voucher for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";
	
	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
		
		VoucherSeriesCollection vc = fc.getVoucherSeriesCollection(yearId);
		VoucherSeriesTable vt = new VoucherSeriesTable(vc);
		
		vt.print(sess.getConsole());
		
		return null;
	}
	
	
}
