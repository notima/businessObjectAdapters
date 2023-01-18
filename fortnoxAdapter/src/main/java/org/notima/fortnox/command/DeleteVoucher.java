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
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "delete-fortnox-voucher", description = "Delete voucher in Fortnox")
@Service
public class DeleteVoucher extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Option(name = "--no-confirm", description = "Don't confirm anything. Default is to confirm", required = false, multiValued = false)
	private boolean noConfirm = false;

	@Option(name = "--yearId", description = "Voucher for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Option(name = "--reverse-date", description = "Date for reversal (if not same as voucher)", required = false, multiValued = false)
	private String reverseDateStr;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	@Argument(index = 1, name = "series", description ="The series", required = true, multiValued = false)
	private String series = "";

	@Argument(index = 2, name = "firstVoucherNo", description ="The voucher no", required = true, multiValued = false)
	private int voucherNo;
	
	@Argument(index = 3, name = "lastVoucherNo", description = "The last voucher no in the series", required = false, multiValued = false)
	private int	lastVoucherNo;
	
	private List<Integer> vouchersToRemove = new ArrayList<Integer>();
	
	
	@Override
	public Object execute() throws Exception {
			
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		int yId = (yearId!=null && yearId.intValue()!=0 ? yearId.intValue() : fc.getFinancialYear(null).getId());

		// Lookup voucher
		Voucher voucher = fc.getVoucher(yId, series, voucherNo);
		
		if (voucher==null) {
			sess.getConsole().println("Voucher " + series + " " + voucherNo + " doesn't exist.");
			return null;
		}
		
		checkParameters();
		
		String reply = noConfirm ? "y" : sess.readLine(confirmQueryString() + "? (y/n) ", null);
		if (reply.equalsIgnoreCase("y")) {
			
			try {
				for (Integer vv : vouchersToRemove) {
					fc.deleteVoucher(yId, series, vv);
					sess.getConsole().println("Voucher " + series + " " + vv + " removed.");
				}
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
		
		return null;
	}
	
	private String confirmQueryString() {
		String result;
		if (vouchersToRemove.size()==1) {
			result = "Do you want to delete voucher " + series + " " + voucherNo;
		} else {
			result = "Do you want to remove vouchers " + series + " " + voucherNo + " to " + lastVoucherNo;
		}
		return result;
	}
	
	private void checkParameters() {
		
		if (lastVoucherNo==0) {
			vouchersToRemove.add(voucherNo);
			return;
		}
		
		if (voucherNo > lastVoucherNo) {
			sess.getConsole().println("Lowest voucher number must be first.");
			return;
		}
		
		for (int voucherToRemove = lastVoucherNo; voucherToRemove >= voucherNo; voucherToRemove--) {
			vouchersToRemove.add(voucherToRemove);
		}
		
	}
	
	
}
