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
import org.notima.api.fortnox.entities3.ModeOfPayment;
import org.notima.api.fortnox.entities3.ModeOfPaymentSubset;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "config-fortnox-mode-of-payment", description = "Configure modes of payment.")
@Service
public class ConfigFortnoxModeOfPayment extends FortnoxCommand implements Action {
	
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Argument(index = 1, name = "mode", description ="The mode of payment code to configure. If it doesn't exist, it's created", required = true, multiValued = false)
	private String mode = "";
	
	@Option(name = "--description", description = "Description of the mode of payment", required = false, multiValued = false)
	private String description;
	
	@Option(name = "--account", description = "The account to tie the mode of payment to", required = false, multiValued = false)
	private Integer account;
	
	
	@Override
	public Object execute() throws Exception {
		
		try {
		
			FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
			ModeOfPayment mp = null;
			ModeOfPaymentSubset ms = fc.getModeOfPayment(mode);
			if (ms!=null) {
				mp = new ModeOfPayment(ms);
			}
			
			if (mp==null) {
				
				String reply = sess.readLine("Mode " + mode + " doesn't exist. Do you want to create it? (y/n) ", null);
				if (reply.equalsIgnoreCase("y")) {
					
					mp = new ModeOfPayment();
					mp.setCode(mode);
					if (account!=null) {
						mp.setAccountNumber(account.toString());
					}
					mp.setDescription(description);
					
					fc.setModeOfPayment(mp);
					sess.getConsole().println("Mode " + mode + " created.");
					
				} else {
					sess.getConsole().println("Operation cancelled.");
				}
				
			} else {
				
				if (description!=null) {
					mp.setDescription(description);
				}
				if (account!=null) {
					mp.setAccountNumber(account.toString());
				}
				fc.setModeOfPayment((ModeOfPayment)mp);
				
			}
			
		} catch (FortnoxException fe) {
			sess.getConsole().println(fe.toString());
		}
		
		return null;
	}
	
}
