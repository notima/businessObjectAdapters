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
import org.notima.api.fortnox.entities3.Vouchers;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.VouchersTable;

@Command(scope = "fortnox", name = "list-fortnox-vouchers", description = "Lists vouchers in Fortnox")
@Service
public class ListVouchers extends FortnoxCommand2 implements Action {

	@Reference 
	Session sess;
	
	@Option(name = "--yearId", description = "Vouchers for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";
	
	@Argument(index = 1, name = "series", description ="The series", required = true, multiValued = false)
	private String series = "";
	
	@Override
	public Object execute() throws Exception {

		bf = this.getBusinessObjectFactoryForOrgNo(orgNo);

		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		
		Vouchers vouchers = fc.getVouchers(yearId, series);
		
		VouchersTable iht = new VouchersTable(vouchers);
		iht.print(sess.getConsole());
			
		sess.getConsole().println("\n" + vouchers.getTotalResources() + " voucher(s).");
		
		return null;
	}
	
	
}
