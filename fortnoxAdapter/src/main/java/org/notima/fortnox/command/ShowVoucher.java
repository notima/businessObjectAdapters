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
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.fortnox.command.table.VoucherTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "show-fortnox-voucher", description = "Show fortnox Voucher")
@Service
public class ShowVoucher extends FortnoxCommand implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference 
	Session sess;
	
	@Option(name = "--yearId", description = "Voucher for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
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

		Voucher voucher = fc.getVoucher(yId, series, voucherNo);
		VoucherTable vt = new VoucherTable(voucher);
		
		vt.print(sess.getConsole());
		
		return null;
	}
	
	
}
