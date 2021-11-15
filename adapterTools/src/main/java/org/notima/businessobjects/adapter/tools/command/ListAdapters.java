package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;

@Command(scope = "notima", name = "list-adapters", description = "Lists registered adapters")
@Service
public class ListAdapters implements Action {

	@Reference
	private Session sess;
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference
	private List<PaymentFactory> pfs;
	
	@Reference
	private List<PaymentBatchProcessor> pbps;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {
	
		if (bofs==null) {
			System.out.println("No adapters registered");
		} else {
			for (BusinessObjectFactory bf : bofs) {

				sess.getConsole().println(bf.getSystemName());
				
			}
			System.out.println(bofs.size() + " adapters registered");
		}
		
		if (pfs==null) {
			System.out.println("No payment factories registered");
		} else {
			for (PaymentFactory pf : pfs) {

				sess.getConsole().println(pf.getSystemName());
				
			}
			System.out.println(pfs.size() + " payment factories registered");
		}

		if (pbps==null) {
			System.out.println("No payment batch processors registered");
		} else {
			for (PaymentBatchProcessor pbp : pbps) {

				sess.getConsole().println(pbp.getSystemName());
				
			}
			System.out.println(pbps.size() + " payment batch processors registered");
		}

		
		return null;
	}

}
