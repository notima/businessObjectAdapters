package org.notima.fortnox.command;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.notima.api.fortnox.FortnoxCredentialsProvider;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.businessobjects.adapter.fortnox.FileCredentialsProvider;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "purge-fortnox-credentials", description = "Purges old credentials for client")
@Service
public class PurgeCredentials extends FortnoxCommand implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Option(name = "--untildate", description = "Purge at maximum until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String	untilDateStr;
	
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	@SuppressWarnings("rawtypes")
	private BusinessObjectFactory bf;	

	private Date		   untilDate = null;
	private long		   untilRefresh = 0;
	
	private FortnoxCredentialsProvider	credentialsProvider;
	private List<FortnoxCredentials>	credentials;

	
	@Override
	public Object execute() throws Exception {

		FactorySelector selector = new FactorySelector(bofs);
		
		bf = selector.getFactoryWithTenant(FortnoxAdapter.SYSTEMNAME, orgNo, null);

		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}
		
		determineUntilRefresh();

		initCredentialsProvider();

		if(credentials == null) {
			sess.getConsole().println("No credentials found");
			return null;
		}
		
		int numberPurged = 0;
		if (confirmPurge()) {
			numberPurged = credentialsProvider.purgeOauthCredentialsUntil(untilRefresh);
			sess.getConsole().println(numberPurged + " credentials removed.");
		}
		
		return null;
	}
	
	private boolean confirmPurge() throws IOException {
		String reply = 
				sess.readLine("Do you really want to remove all credentials older than " 
						+ dateFormat.format(untilDate) + " for " 
						+ bf.getCurrentTenant().getName() + "(y/n) ? ", null);
		if (reply!=null && reply.toLowerCase().startsWith("y")) {
			return true;
		}
		return false;
	}
	
	private void initCredentialsProvider() throws Exception {
		credentialsProvider = new FileCredentialsProvider(orgNo);
		credentials = credentialsProvider.getAllCredentials();
	}
	
	private void determineUntilRefresh() throws ParseException {
		if (untilDateStr!=null) {
			untilDate = FortnoxClient3.s_dfmt.parse(untilDateStr);
		}
		
		if (untilDate!=null) {
			untilRefresh = untilDate.getTime();
		} else {
			Calendar now = Calendar.getInstance();
			now.add(Calendar.DATE, -31);
			untilRefresh = now.getTimeInMillis();
		}
	}
	
	
}
