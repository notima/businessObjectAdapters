package org.notima.fortnox.command;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherRow;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "create-fortnox-voucher", description = "Create a Fortnox voucher")
@Service
@SuppressWarnings("rawtypes")
public class CreateVoucher extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Option(name = "--date", description = "Accounting date (unless today)", required = false, multiValued = false)
	private String dateStr;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";
	
	@Argument(index = 1, name = "series", description ="The series", required = true, multiValued = false)
	private String series = "";

	@Argument(index = 2, name = "creditAcct", description ="From account (credit)", required = true, multiValued = false)
	private String creditAcct;
	
	@Argument(index = 3, name = "debetAcct", description ="To account (debet)", required = true, multiValued = false)
	private String debetAcct;

	@Argument(index = 4, name = "amount", description ="The amount to account", required = true, multiValued = false)
	private Double amount;
	
	@Argument(index = 5, name = "txt", description ="Voucher text describing the transaction", required = true, multiValued = false)
	private String revTxt;
	
	
	@Override
	public Object execute() throws Exception {

		FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		Date acctDate = null;
		if (dateStr!=null) {
			acctDate = FortnoxClient3.s_dfmt.parse(dateStr);
		} else {
			acctDate = Calendar.getInstance().getTime();
			dateStr = FortnoxClient3.s_dfmt.format(acctDate);
		}
		
		
		int yId = fc.getFinancialYear(acctDate).getId();
		
		if (yId==0) {
			sess.getConsole().println("No financial year for " + dateStr);
			return null;
		}
		
		// Check for closed period
		Date lockedUntil = fc.getLockedPeriodUntil();
		if (lockedUntil!=null) {
			if (acctDate.before(lockedUntil)) {
				sess.getConsole().println("Accounting is locked until " + FortnoxClient3.s_dfmt.format(lockedUntil));
				return null;
			}
		}

		Voucher voucher = new Voucher();
		voucher.setVoucherSeries(series);
		voucher.setDescription(revTxt);
		voucher.setTransactionDate(dateStr);
		VoucherRow vr = new VoucherRow();
		vr.setAccount(Integer.parseInt(creditAcct));
		vr.setCredit(amount);
		voucher.addVoucherRow(vr);
		vr = new VoucherRow();
		vr.setAccount(Integer.parseInt(debetAcct));
		vr.setDebit(amount);
		voucher.addVoucherRow(vr);
		
		String reply = sess.readLine("Do you want to create voucher in series " + series + " with amount " + amount + " on " + voucher.getTransactionDate() + " (y/n) ", null);

		if ("y".equalsIgnoreCase(reply)) {

			Voucher result = fc.setVoucher(voucher);
			if (result!=null) {
				sess.getConsole().println("Created voucher " + result.getVoucherSeries() + " : " + result.getVoucherNumber());
			}
			
		}
		
		return null;
	}
	
	
	
}
