package org.notima.fortnox.command;

import java.util.Date;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.FinancialYearSubset;
import org.notima.fortnox.command.table.RevenueAccountMapTable;

@Command(scope = "fortnox", name = "show-revenue-account-map", description = "Show revenue account map for given client.")
@Service
public class ShowRevenueAccountMap extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Option(name = "--fromdate", description = "Use this date to determine what year show account map for. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDate;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	private FinancialYearSubset fy;
	
	private Map<String, Integer> revenueAccountMap;
	
	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		Date useDate = null;
		if (fromDate!=null) {
			useDate = FortnoxClient3.s_dfmt.parse(fromDate);
		}

		fy = fc.getFinancialYear(useDate);
		int yearId = fy.getId();
		
		revenueAccountMap = fc.getRevenueAccountMap(yearId);
		
		sess.getConsole().println("Financial year: " + fy.getFromDate() + " - " + fy.getToDate());
		
		RevenueAccountMapTable at = new RevenueAccountMapTable(revenueAccountMap);
		
		at.getShellTable().print(sess.getConsole());
		
		return null;
	}

	
	
	
	
}
