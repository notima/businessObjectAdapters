package org.notima.fortnox.command;

import java.util.ArrayList;
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
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.CustomerSubset;
import org.notima.api.fortnox.entities3.Customers;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.CustomerHeaderTable;

@Command(scope = "fortnox", name = "list-fortnox-customers", description = "Lists customers Fortnox")
@Service
public class ListCustomers extends FortnoxCommand2 implements Action {

	@Reference 
	Session sess;
	
	
	@Option(name = "-e", aliases = {
	"--enrich" }, description = "Read the complete customer record, not just the subset", required = false, multiValued = false)
	private boolean enrich = false;
	
	@Option(name = "--inactive", description = "List inactive customers", required = false, multiValued = false)
	private boolean inactive = false;
	
	@Option(name = "--top", description = "With given terms of payment", required = false, multiValued = false)
	private String top;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";
	
	@Override
	public Object execute() throws Exception {

		
		bf = this.getBusinessObjectFactoryForOrgNo(orgNo);

		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}
		
			
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		Customers customers = fc.getCustomers(inactive ? FortnoxClient3.FILTER_INACTIVE : FortnoxClient3.FILTER_ACTIVE);
		
		if (top!=null) {
			// Looking for terms of payment demands enrich
			enrich = true;
		}
		
		if (customers.getCustomerSubset()!=null) {

			Customer c;
			String termsOfPayment = null;
			List<Object> ps = new ArrayList<Object>();
			for (CustomerSubset cs : customers.getCustomerSubset()) {
				
				if (enrich) {
					c = fc.getCustomerByCustNo(cs.getCustomerNumber());
					if (top!=null) {
						termsOfPayment = c.getTermsOfPayment();
						if (!top.equalsIgnoreCase(termsOfPayment)) {
							continue;
						}
					}
					ps.add(c);
				} else {
					ps.add(cs);
				}
			}
			
			CustomerHeaderTable iht = new CustomerHeaderTable(ps);
			iht.getShellTable().print(sess.getConsole());
			
			sess.getConsole().println("\n" + ps.size() + " customer(s).");
			
		}
		
		return null;
	}
	
	
}
