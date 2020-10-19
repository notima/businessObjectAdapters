package org.notima.fortnox.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * 
 * Removes a fortnox client.
 * 
 * @author Daniel Tamm
 *
 */
@Command(scope = "fortnox", name = "remove-client", description = "Remove a client to Fortnox integration")
@Service
public class RemoveClient implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference
	private Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client to remove", required = true, multiValued = false)
	String orgNo;
    
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		BusinessObjectFactory b = null;
		for (BusinessObjectFactory bf : bofs) {
			if ("Fortnox".equals(bf.getSystemName())) {
				b = bf;
				break;
			}
		}

		if (b==null) {
			sess.getConsole().println("No Fortnox adapter available");
			return null;
		}
		
		b.setTenant(orgNo, null);
		BusinessPartner<?> bp = b.getCurrentTenant();
		if (bp==null) {
			sess.getConsole().println("No client [" + orgNo + "] found.");
			return null;
		}
		
		boolean result = false;
		
		String reply = sess.readLine("Do you really want to remove client [ " + orgNo + " ] " + bp.getName() + "? (y/n) ", null);
		if (reply.equalsIgnoreCase("y")) {
			result = b.removeTenant(orgNo, null);
		} else {
			sess.getConsole().println("Cancelled.");
			return null;
		}
		

		if (!result) {
			sess.getConsole().println("Removal unsucessful for [" + orgNo + "].");
		} else {
			sess.getConsole().println(bp.getTaxId() + " removed.");
		}

		return null;
	}
	
}
