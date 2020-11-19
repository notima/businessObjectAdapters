package org.notima.fortnox.command;

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
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "cancel-fortnox-voucher", description = "Process a Fortnox voucher")
@Service
@SuppressWarnings("rawtypes")
public class CancelVoucher extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Reference
	private List<BusinessObjectFactory> bofs;
	
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
	
	@Argument(index = 3, name = "revTxt", description ="The text explaining the reversal", required = false, multiValued = false)
	private String revTxt;
	
	
	@Override
	public Object execute() throws Exception {

		FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
	
		if (revTxt==null) {
			revTxt = "REVERSAL";
		}

		
		int yId = (yearId!=null && yearId.intValue()!=0 ? yearId.intValue() : fc.getFinancialYear(null).getId());

		Voucher voucher = fc.getVoucher(yId, series, voucherNo);

		Date reverseDate = null;
		if (reverseDateStr!=null) {
			reverseDate = FortnoxClient3.s_dfmt.parse(reverseDateStr);
		} else {
			reverseDate = FortnoxClient3.s_dfmt.parse(voucher.getTransactionDate());
			reverseDateStr = voucher.getTransactionDate();
		}
		
		// Check for closed period
		Date lockedUntil = fc.getLockedPeriodUntil();
		if (lockedUntil!=null) {
			if (reverseDate.before(lockedUntil)) {
				sess.getConsole().println("Accounting is locked until " + FortnoxClient3.s_dfmt.format(lockedUntil));
				return null;
			}
		}

		if (voucher!=null) {

			Voucher v = new Voucher();
			v.reverse(voucher, revTxt);
			if (reverseDate!=null) {
				v.setTransactionDate(reverseDateStr);
			} else {
				v.setTransactionDate(voucher.getTransactionDate());
			}
			
			String reply = sess.readLine("Do you want to cancel (reverse) voucher " + series + " " + voucherNo + " on " + v.getTransactionDate() + " (y/n) ", null);

			if ("y".equalsIgnoreCase(reply)) {

				Voucher result = fc.setVoucher(v);
				if (result!=null) {
					sess.getConsole().println("Created voucher " + result.getVoucherSeries() + " : " + result.getVoucherNumber());
				}
				
			}
			
		}
		
		return null;
	}
	
	
	
}
