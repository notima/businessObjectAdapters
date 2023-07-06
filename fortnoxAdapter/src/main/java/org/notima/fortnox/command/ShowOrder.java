package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Order;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.OrderHeaderTable;
import org.notima.fortnox.command.table.OrderLineTable;

@Command(scope = "fortnox", name = "show-fortnox-order", description = "Show a specific order")
@Service
public class ShowOrder extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	@Argument(index = 1, name = "orderNo", description ="The order no", required = true, multiValued = false)
	private String orderNo;
	
	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		Order order = fc.getOrder(orderNo);

		if (order==null) {
			sess.getConsole().println("Order " + orderNo + " not found.");
			return null;
		}
		
		OrderHeaderTable iht = new OrderHeaderTable(order);
		OrderLineTable ilt = new OrderLineTable(order, null);
		
		iht.getShellTable().print(sess.getConsole());
		ilt.print(sess.getConsole());
		
		return null;
	}
	
	
}
