package org.notima.fortnox.command;

import java.util.ArrayList;
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
import org.notima.api.fortnox.entities3.Account;
import org.notima.api.fortnox.entities3.AccountSubset;
import org.notima.api.fortnox.entities3.Accounts;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.AccountTable;
import org.notima.generic.businessobjects.AccountClass;
import org.notima.generic.businessobjects.BasicAccountingReportProvider;

@Command(scope = "fortnox", name = "show-fortnox-coa", description = "List chart of accounts for given client.")
@Service
public class ShowChartOfAccounts extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Option(name = "--all", description = "Include in active accounts", required = false, multiValued = false)
	private boolean showInactive;
	
	@Option(name = "--balances", description = "Include balances (takes longer time)", required = false, multiValued = false)
	private boolean showBalances;
	
	@Option(name = "--only-with-balances", description = "Include only accounts with balances (implicits --balances)", required = false, multiValued = false)
	private boolean onlyWithBalances;
	
	@Option(name = "--only-with-vatcodes", description = "Include only accounts with VAT-codes (implicits --include-pl-accounts)", required = false, multiValued = false)
	private boolean onlyWithVatCodes;
	
	@Option(name = "--yearId", description = "Show COA for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Option(name = "--include-pl-accounts", description = "Include P/L accounts (not just balance sheet accounts)", required = false, multiValued = false)
	private boolean includePlAccounts;
	
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	private BasicAccountingReportProvider barp = new BasicAccountingReportProvider();
	
	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
		
		if (yearId==null) {
			yearId = fc.getFinancialYear(null).getId();
		}
		
		Accounts accts = fc.getAccounts(yearId);
		if (accts==null) {
			sess.getConsole().println("No chart of accounts found for yearId " + yearId);
			return null;
		}
		
		Account acct;
		String acctNo;

		if (onlyWithVatCodes) {
			includePlAccounts = true;
		}
		
		List<Object> acctList = new ArrayList<Object>();
		for (AccountSubset as : accts.getAccountSubset()) {
			acctNo = as.getNumber().toString();
			
			if (!includePlAccounts) {
				// Skip revenue accounts
				if (!AccountClass.isBalanceClass(barp.getAccountClass(acctNo))) {
					continue;
				}
			}

			if (onlyWithVatCodes) {
				// Skip accounts without VAT-code
				if (as.getVATCode()==null || as.getVATCode().trim().length()==0) {
					continue;
				}
			}
			
			if (as.getActive() || showInactive) {
				
				if (showBalances || onlyWithBalances) {
					// TODO: Lookup of individual account
					acct = fc.getAccount(yearId, as.getNumber());
					
					if (onlyWithBalances) {
						if (acct.getBalanceBroughtForward()!=0 || acct.getBalanceCarriedForward()!=0) {
							acctList.add(acct);
						}
					} else {
						acctList.add(acct);
					}
				} else {
					acctList.add(as);
				}
			}
		}
		
		AccountTable at = new AccountTable(acctList);
		
		at.print(sess.getConsole());
		
		return null;
	}

	
	
	
	
}
