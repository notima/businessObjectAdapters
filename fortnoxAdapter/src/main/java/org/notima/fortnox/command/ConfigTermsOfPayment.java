package org.notima.fortnox.command;

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
import org.notima.api.fortnox.entities3.TermsOfPayment;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "config-terms-of-payment", description = "Configure terms of payment (adds / changes description)")
@Service
public class ConfigTermsOfPayment extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)
	private String orgNo = "";

	@Argument(index = 1, name = "term", description ="The term of payment code to configure. If it doesn't exist, it's created", required = true, multiValued = false)
	private String term = "";
	
	@Option(name = "--description", description = "Description of the mode of payment", required = false, multiValued = false)
	private String description;
	
	@Override
	public Object execute() throws Exception {
		
		try {
		
			FortnoxClient3 fc = getFortnoxClient(orgNo);
			if (fc == null) {
				sess.getConsole().println("Can't get client for " + orgNo);
				return null;
			}
			
			TermsOfPayment mp = fc.getTermsOfPaymentByCode(term);
			if (mp!=null) {
				mp = new TermsOfPayment(mp);
			}
			
			if (mp==null) {
				
				String reply = sess.readLine("Term " + term + " doesn't exist. Do you want to create it? (y/n) ", null);
				if (reply.equalsIgnoreCase("y")) {
					
					mp = new TermsOfPayment();
					mp.setCode(term);
					mp.setDescription(description);
					
					fc.setTermsOfPayment(mp);
					sess.getConsole().println("Term " + term + " created.");
					
				} else {
					sess.getConsole().println("Operation cancelled.");
				}
				
			} else {
				
				if (description!=null) {
					mp.setDescription(description);
				}
				fc.setTermsOfPayment((TermsOfPayment)mp);
				
			}
			
		} catch (FortnoxException fe) {
			sess.getConsole().println(fe.toString());
		}
		
		return null;
	}
	
}
