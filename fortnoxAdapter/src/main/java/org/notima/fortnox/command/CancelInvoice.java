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
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "cancel-fortnox-invoice", description = "Cancels an invoice in Fortnox")
@Service
public class CancelInvoice extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Option(name = "--no-confirm", description = "Don't confirm anything. Default is to confirm", required = false, multiValued = false)
	private boolean noConfirm = false;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	@Argument(index = 1, name = "firstInvoiceNo", description ="The invoice no", required = true, multiValued = false)
	private int invoiceNo;
	
	@Argument(index = 2, name = "lastInvoiceNo", description = "The last invoice no in the series", required = false, multiValued = false)
	private int	lastInvoiceNo;
	
	private List<Integer> invoicesToCancel = new ArrayList<Integer>();
	
	
	@Override
	public Object execute() throws Exception {
			
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		// Lookup voucher
		Invoice invoice = fc.getInvoice(Integer.toString(invoiceNo));
		
		if (invoice==null) {
			sess.getConsole().println("Invoice " + invoiceNo + " doesn't exist.");
			return null;
		}
		
		checkParameters();
		
		String reply = noConfirm ? "y" : sess.readLine(confirmQueryString() + "? (y/n) ", null);
		if (reply.equalsIgnoreCase("y")) {
			
			try {
				for (Integer vv : invoicesToCancel) {
					fc.cancelInvoice(vv.toString());
					sess.getConsole().println("Invoice " + vv + " cancelled.");
				}
			} catch (Exception e) {
				String msg = null;
				if (e instanceof FortnoxException) {
					msg = ((FortnoxException)e).getMessage();
				} else {
					msg = e.getMessage();
				}
				sess.getConsole().println("Cancel failed: " + msg);
				return null;
			}
			
		} else {
			sess.getConsole().println("Cancel cancelled");
			return null;
		}
		
		return null;
	}
	
	private String confirmQueryString() {
		String result;
		if (invoicesToCancel.size()==1) {
			result = "Do you want to cancel invoice " + invoiceNo;
		} else {
			result = "Do you want to remove invoices " + invoiceNo + " to " + lastInvoiceNo;
		}
		return result;
	}
	
	private void checkParameters() {
		
		if (lastInvoiceNo==0) {
			invoicesToCancel.add(invoiceNo);
			return;
		}
		
		if (invoiceNo > lastInvoiceNo) {
			sess.getConsole().println("Lowest invoice number must be first.");
			return;
		}
		
		for (int voucherToRemove = lastInvoiceNo; voucherToRemove >= invoiceNo; voucherToRemove--) {
			invoicesToCancel.add(voucherToRemove);
		}
		
	}
	
	
}
