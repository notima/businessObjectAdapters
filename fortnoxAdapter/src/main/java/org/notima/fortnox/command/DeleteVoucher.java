package org.notima.fortnox.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "delete-fortnox-voucher", description = "Delete voucher in Fortnox")
@Service
@SuppressWarnings("rawtypes")
public class DeleteVoucher extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Option(name = "--no-confirm", description = "Don't confirm anything. Default is to confirm", required = false, multiValued = false)
	private boolean noConfirm = false;

	@Option(name = "--yearId", description = "Voucher for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Option(name = "--reverse-date", description = "Date for reversal (if not same as voucher)", required = false, multiValued = false)
	private String reverseDateStr;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Argument(index = 1, name = "series", description ="The series", required = true, multiValued = false)
	private String series = "";

	@Argument(index = 2, name = "voucherNo", description ="The voucher no", required = true, multiValued = false)
	private int voucherNo;
	
	
	@Override
	public Object execute() throws Exception {
			
		FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
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
		
		String reply = noConfirm ? "y" : sess.readLine("Do you want to delete voucher " + series + " " + voucherNo + "? (y/n) ", null);
		if (reply.equalsIgnoreCase("y")) {
			
			try {
				fc.deleteVoucher(yId, series, voucherNo);
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
	
	
}
