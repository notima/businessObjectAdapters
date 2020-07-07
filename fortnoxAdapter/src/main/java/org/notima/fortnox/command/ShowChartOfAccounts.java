package org.notima.fortnox.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Account;
import org.notima.api.fortnox.entities3.AccountSubset;
import org.notima.api.fortnox.entities3.Accounts;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.fortnox.command.table.AccountTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "show-fortnox-coa", description = "List chart of accounts for given client.")
@Service
@SuppressWarnings("rawtypes")
public class ShowChartOfAccounts implements Action {

	@Reference 
	Session sess;
	
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Option(name = "--all", description = "Include in active accounts", required = false, multiValued = false)
	private boolean showInactive;
	
	@Option(name = "--balances", description = "Include balances (takes longer time)", required = false, multiValued = false)
	private boolean showBalances;
	
	@Option(name = "--only-with-balances", description = "Include only accounts with balances (implicits --balances)", required = false, multiValued = false)
	private boolean onlyWithBalances;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Override
	public Object execute() throws Exception {

		FactorySelector selector = new FactorySelector(bofs);
		
		BusinessObjectFactory bf = selector.getFactoryWithTenant(FortnoxAdapter.SYSTEMNAME, orgNo, null);

		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}
		
		FortnoxAdapter fa = (FortnoxAdapter)bf;
		
		FortnoxClient3 fc = fa.getClient();
		int yearId = fc.getFinancialYear(null).getId();
		
		Accounts accts = fa.getClient().getAccounts(yearId);
		Account acct;
		
		List<Object> acctList = new ArrayList<Object>();
		if (showInactive) {
			acctList.addAll(accts.getAccountSubset());
		} else {
			for (AccountSubset as : accts.getAccountSubset()) {
				if (as.getActive()) {
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
		}
		
		AccountTable at = new AccountTable(acctList);
		
		at.print(sess.getConsole());
		
		return null;
	}

	
	
	
	
}
