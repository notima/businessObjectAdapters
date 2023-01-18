package org.notima.fortnox.command;

import java.io.File;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.FortnoxFile;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherFileConnection;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "attach-file-to-voucher", description = "Attach a file to a voucher.")
@Service
public class AttachFileToVoucher extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Option(name = "--yearId", description = "Voucher for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)
	private String orgNo = "";
	
	@Argument(index = 1, name = "series", description ="The series", required = true, multiValued = false)
	private String series = "";
	
	@Argument(index = 2, name = "voucherNo", description ="The voucher no", required = true, multiValued = false)
	private int voucherNo;
	
    @Argument(index = 3, name = "file", description = "The full file path", required = true, multiValued = false)
    @Completion(FileCompleter.class)
    private String fileName;
	
	@Override
	public Object execute() throws Exception {

		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		// Check that the file exists
		File file = new File(fileName);
		if (!file.exists()) {
			sess.getConsole().printf("File %s not found.\n", fileName);
			return null;
		}
		
		int yId = (yearId!=null && yearId.intValue()!=0 ? yearId.intValue() : fc.getFinancialYear(null).getId());

		Voucher v = fc.getVoucher(yId, series, voucherNo);
		
		FortnoxFile ff = fc.uploadFile(fileName, FortnoxClient3.INBOX_VOUCHERS);
		
		VoucherFileConnection vfc = fc.setVoucherFileConnection(
				ff.getId(), 
				Integer.toString(v.getVoucherNumber()), 
				v.getVoucherSeries(), 
				FortnoxClient3.s_dfmt.parse(v.getTransactionDate()));
		
		return vfc.getFileId();
		
	}
	
	
	
}
