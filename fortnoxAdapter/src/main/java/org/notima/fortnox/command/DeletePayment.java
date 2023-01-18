package org.notima.fortnox.command;

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
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "delete-fortnox-payment", description = "Delete payment in Fortnox")
@Service
public class DeletePayment extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Option(name = "--no-confirm", description = "Don't confirm anything. Default is to confirm", required = false, multiValued = false)
	private boolean noConfirm = false;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";
	
	@Argument(index = 1, name = "paymentNo", description ="The payment(s) to be removed", required = true, multiValued = true)
	private List<Integer> paymentNos;
	
	
	@Override
	public Object execute() throws Exception {
			
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
		
		InvoicePayment pmt;
		
		for (Integer no : paymentNos) {
			
			// Lookup payment
			pmt = fc.getInvoicePayment(no);
			if (pmt==null) {
				sess.getConsole().println("Payment # " + no + " doesn't exist.");
				continue;
			}
			
			String reply = noConfirm ? "y" : sess.readLine("Do you want to delete payment # " + no + " for " + pmt.getInvoiceCustomerName() + "? (y/n) ", null);
			if (reply.equalsIgnoreCase("y")) {
				
				try {
					fc.deleteInvoicePayment(no);
				} catch (Exception e) {
					String msg = null;
					if (e instanceof FortnoxException) {
						msg = ((FortnoxException)e).getMessage();
					} else {
						msg = e.getMessage();
					}
					sess.getConsole().println("Delete failed: " + msg);
					return null;
				}
				
			} else {
				sess.getConsole().println("Delete cancelled");
				return null;
			}
			
		}
		
		return null;
	}
	
	
}
