package org.notima.fortnox.command;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "process-fortnox-voucher", description = "Process a Fortnox voucher")
@Service
@SuppressWarnings("rawtypes")
public class ProcessVoucher implements Action {

	@Reference 
	Session sess;
	
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client to send the voucher", required = true, multiValued = false)
	String orgNo = "";
	
	@Argument(index = 1, name = "voucherFile", description ="The voucher file to process (in Fortnox XML-format)", required = true, multiValued = false)
	@Completion(FileCompleter.class)	
	
	String voucherFile = "";
	
	@Override
	public Object execute() throws Exception {

		FactorySelector selector = new FactorySelector(bofs);
		
		BusinessObjectFactory bf = selector.getFactoryWithTenant(FortnoxAdapter.SYSTEMNAME, orgNo, null);
		
		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}
		
		// Try to open the file
		File f = new File(voucherFile);
		if (!f.canRead()) {
			sess.getConsole().println("File " + voucherFile + " can't be found.");
			return null;
		}
		
		if (bf instanceof FortnoxAdapter) {
			FortnoxAdapter fa = (FortnoxAdapter)bf;
			try {
				Voucher v = JAXB.unmarshal(f, Voucher.class);

				fa.setTenant(orgNo, null);
				Voucher result = fa.getClient().setVoucher(v);
				if (result!=null) {
					sess.getConsole().println("Created voucher " + result.getVoucherSeries() + " : " + result.getVoucherNumber());
				}
				
			} catch (Exception e) {
				sess.getConsole().println(e.getMessage());
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	
	
}
