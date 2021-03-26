package org.notima.fortnox.command;

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
import org.notima.api.fortnox.entities3.Account;
import org.notima.fortnox.command.completer.ConfigFortnoxAccountCompleter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "config-fortnox-account", description = "Configure an account in the chart of accounts.")
@Service
public class ConfigFortnoxAccount extends FortnoxCommand implements Action {

	public static final String CONF_ENABLED = "enabled";
	public static final String CONF_NAME = "name";
	public static final String CONF_VATCODE = "vatCode";
	
	public static String[] configs = new String[] {
			CONF_ENABLED,
			CONF_NAME,
			CONF_VATCODE
	};
	
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference 
	Session sess;
	
	@Option(name = "--yearId", description = "Config account for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Argument(index = 1, name = "accountNo", description ="The account number to configure.", required = true, multiValued = false)
	private String accountNo = "";
	
    @Argument(index = 2, name = "configuration parameter", description = "What to configure (name, enabled)", required = true, multiValued = false)
    @Completion(ConfigFortnoxAccountCompleter.class)
    String key;

    @Argument(index = 3, name = "value", description = "Value provided to configuration. If omitted, value is set to null", required = false, multiValued = false)
    String value;
	
	@Override
	public Object execute() throws Exception {

		// Make sure key is recognized
		boolean keyOk = false;
		for (String c : configs) {
			if (c.equalsIgnoreCase(key)) {
				keyOk = true;
				break;
			}
		}
		
		if (!keyOk) {
			sess.getConsole().println("Configuration parameter '" + key + "' not recognized");
			return null;
		}

		Boolean toggle = null;
		try {
			toggle = value==null ? false : Boolean.parseBoolean(value);
		} catch (Exception e) {
		}
		
		try {
		
			FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
			if (fc == null) {
				sess.getConsole().println("Can't get client for " + orgNo);
				return null;
			}
			
			if (yearId==null) {
				yearId = fc.getFinancialYear(null).getId();
			}
	
			Account acct = fc.getAccount(yearId, Integer.parseInt(accountNo));
			
			
			if (CONF_ENABLED.equalsIgnoreCase(key)) {

				if (acct==null) {
					sess.getConsole().println("Account " + accountNo + " not found for yearId: " + yearId);
					return null;
				}
				
				if (toggle!=null) {
					if ((acct.getActive() && toggle==false) || 
							(!acct.getActive() && toggle==true)) {
					
						acct.setActive(toggle);
						fc.updateAccount(yearId, acct);
						sess.getConsole().println("Enabled set to " + toggle);
					} else {
						sess.getConsole().println("No change.");
					}
					
				}
			}
			
			if (CONF_NAME.equalsIgnoreCase(key)) {
				
				if (acct==null) {
					// The account should be created
					acct = new Account();
					acct.setNumber(Integer.parseInt(accountNo));
					acct.setActive(true);
					sess.getConsole().println("Creating account " + accountNo);
				} 
				acct.setDescription(value);
				
				fc.updateAccount(yearId, acct);
				
			}
			
			if (CONF_VATCODE.equalsIgnoreCase(key)) {

				if (acct==null) {
					sess.getConsole().println("Account " + accountNo + " not found for yearId: " + yearId);
					return null;
				}
				
				acct.setVATCode(value);
				
				fc.updateAccount(yearId, acct);
				
			}
			
		} catch (FortnoxException fe) {
			sess.getConsole().println(fe.toString());
		}
		
		return null;
	}
	
}
